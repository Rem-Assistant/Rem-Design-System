# Exact provider assets

`brand-assets.json` is the authoritative file/node/dimension/SHA-256 manifest. These are isolated static asset node exports, not cropped screen renders. They were exported read-only with Figma `node.screenshot({scale:4, contentsOnly:true})` after original asset URLs returned empty WAF challenge responses. No redraw, recolor, crop or canvas mutation occurred. PNG alpha is retained.

| File | Designed art size | Containing slot | Use |
|---|---|---|---|
| gmail.png | 26×26 | 26×29 centered | Connector rows / Gmail lockup exact mark |
| google-calendar.png | 26×26 | 26×29 centered | Connector row |
| notion.png | 26×26 | 26×29 centered | Connector row |
| slack.png | 26×26 | 26×29 centered | Connector row |
| google-drive.png | 26×26 | 26×29 centered | Connector row |
| linear.png | 26×26 | 26×29 centered | Connector row |
| todoist.png | 20×20 | 29×29 centered | Connector row; do not enlarge art to fill slot |
| shop-pay.png | 29×29 | 29×29 | Wallet row |
| link-consent.png | 60×60 | 60×60 | Link consent hero; same image-fill source used in 29pt Wallet row |
| shop-pay-consent.png | 60×60 | 60×60 | Shop Pay consent hero |

All files contain 4× the source pixel dimensions (104, 80, 116 or 240 square). The consent logos already include their designed rounded corners and brand backgrounds. Render with aspect fit and the precise documented slot size. Native SF utility symbols remain code, not PNG exports.

Gmail lockup has a 64×64 Visual slot with the 26×26 provider mark centered; do not enlarge it to a 64pt logo. Wallet's row brand marks are historical image-filled ContainedIcon instances; **do not apply Subtle utility-icon fill transformations to these brand images**.

Two endpoint behaviors were handled explicitly: one screenshot response returns at most five images; one Link row-node screenshot emitted no image. Image identity was checked visually and output names corrected. Final manifest records only the verified files and source nodes above.
