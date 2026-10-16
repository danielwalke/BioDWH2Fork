# KEGG data source

BioDWH2 module for [KEGG](https://www.kegg.jp). Besides the entities and relations from the KEGG flat files and REST
API, the exported graph stores **where an element is drawn in the KEGG module and pathway diagrams**, so that
modules and pathways can be laid out like their KEGG images.

## Diagram coordinates on edges

Edges between modules/pathways and the elements drawn in their diagrams carry these properties:

| Property | Type | Meaning |
|---|---|---|
| `layout` | string | Which image the coordinates refer to: `module_ko`, `module_reaction` or `pathway` |
| `x`, `y` | `double[]` | Center of the element in image pixels, origin top left |
| `width`, `height` | `double[]` | Size of the element in pixels |

The four arrays are parallel and hold **one entry per place the element is drawn** in that diagram (a compound or KO
can appear several times). Edges whose element is not drawn simply have no such properties.

| Edge | Coordinates from | Edges with coordinates |
|---|---|---|
| Module → Reaction (`ASSOCIATED_WITH_REACTION`) | module reaction image | 3,211 of 3,243 (99%) |
| Module → Compound (`ASSOCIATED_WITH_COMPOUND`) | module reaction image | 3,934 of 3,937 (~100%) |
| Module → Protein/KO (`ASSOCIATED_WITH_PROTEIN`) | module KO image | 4,234 of 4,234 (100%) |
| Protein/KO → Pathway (`ASSOCIATED_WITH_PATHWAY`) | pathway KGML | 38,154 of 49,070 (78%) |
| Compound → Pathway (`ASSOCIATED_WITH_PATHWAY`) | pathway KGML | 18,857 of 19,693 (96%) |
| Reaction → Pathway (`ASSOCIATED_WITH_PATHWAY`) | pathway KGML | 7,233 of 19,922 (36%) |

Counts are from an export of KEGG version 2026.10.13.160830.

**Edges without coordinates**

- Module → Pathway: no KEGG diagram places a module inside a pathway.
- Pathway → Compound (from the pathway flat file, 130 edges): all 75 pathways involved have no KGML. The same
  compounds do get coordinates through the Compound → Pathway link above.
- A pathway can list a KO or reaction that its diagram does not draw. This is why only 36% of the Reaction → Pathway
  edges have coordinates.
- 84 of 587 pathways have no KGML (mostly overview maps, e.g. `map010xx`, `map070xx`) and therefore no coordinates.
- Organism, drug, disease and reference edges are not diagram based.

Reaction → Pathway edges are created from `pathway_reaction.tsv`, which was downloaded before but not used.

### Where the coordinates come from

- **Modules**: the HTML image maps on `https://www.kegg.jp/module/Mxxxxx`. Map `module1` belongs to the KO-level
  image (`layout=module_ko`), map `reaction1` to the reaction/compound-level image (`layout=module_reaction`).
  KEGG only serves the diagram to browser-like clients, so the updater sends a browser `User-Agent`.
  For polygon areas (compounds) the center and size of the bounding box are used.
- **Pathways**: KGML of the reference layout, `https://rest.kegg.jp/get/ko<number>/kgml`. The position of an enzyme
  entry is used for each of its KOs and for its reactions.

The updater (`KeggUpdater`) flattens both into `module_layout.tsv` and `pathway_layout.tsv` (~200 ms per request,
about 1,170 requests). `KeggLayoutParser` parses the HTML/KGML, `KeggLayout` loads the files and builds the edge
properties, and `KeggGraphExporter` attaches them.

## Parallel edges

Module edges used to be created twice (see below). The exporter now creates **one edge per (module, element)
pair**, with these exceptions that carry different information:

- A reaction or compound that occurs at several steps of a module keeps one edge per step, each with its own `order`
  (`modules.txt` edges, 5 Module → Reaction and 288 Module → Compound pairs).
- A compound that is the product of one step and the substrate of the next gets the same `order` twice; only one
  edge is kept.

How it works: edges from `modules.txt` (with `order`) are created first. The link files (`link/module/...`) are
exported afterwards and skip pairs that are already connected. Link-file edges are only kept for pairs that
`modules.txt` does not produce (61 Module → Reaction and 71 Module → Compound pairs, and all Module → Protein
pairs), and these have no `order`. The skip applies to every link file, so pairs that KEGG itself lists twice
(e.g. `K00331` in `M00144`) are exported once as well.

History: the link-file edges came first (commit `2a49d82a`), the `modules.txt` edges with `order` were added two
days later (`64d9487f`) without guarding the earlier path, which produced the duplicates.

## Known issues

- `KeggUpdater.expectedFileNames()` lists files that are never downloaded (organisms, per-organism files), so every
  update re-downloads all of KEGG. Use `--skip-update` to re-export from already downloaded files.
