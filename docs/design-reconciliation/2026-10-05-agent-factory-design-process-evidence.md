# Agent Factory design process evidence — 2026-10-05

This is a claim-safe evidence record for Samuel's October 7 article. It records what happened in the
Rem design-system program, what the current artifacts prove, and which broader conclusions remain
unverified. It is source material, not a finished article.

## Working thesis

The useful system was not one agent that designed an entire product. It was a product designer
directing several narrow design lanes, then turning repeated corrections into repository-owned
instructions and review gates. The human remained responsible for product judgment. The system's
job became preserving context, exposing gaps, and making the next pass more reliable.

An alternative framing is **Factory Analyst**: the work is the repeated cycle of inspecting an
agent's output, identifying the class of failure, deciding whether it is a product decision or a
workflow defect, and encoding only the reusable part.

## Observed failure

The first Onboarding review declared the topology complete because it counted states and validated
connector geometry. That result was false at the screen-design layer. Twenty-one states were
hand-built facsimiles: ad hoc frames, text, and rectangles that visually suggested Rem screens but
were neither approved source screens nor editable instances of canonical components.

The review had measured graph coverage while silently assuming screen provenance. Samuel's visual
inspection caught the mismatch. This is the clearest example of why a machine-readable count is
useful but insufficient: the topology could be structurally correct while the design artifact was
fake.

## Repair performed

The repair separated the artifacts by responsibility:

- **Figma Design** owns editable canonical screens and components.
- **FigJam** owns topology, branches, connectors, and reference projections.
- **Competitive Audits** remain research evidence on one FigJam page, grouped by source app.
- **Code implementation** is a later phase and is currently deferred by Samuel.

For Onboarding, nineteen missing states were reconciled to existing canonical Figma screens. Two
remaining product gaps—Google sign-in and Activation—were created as editable proposed Figma
components, visually reviewed, and then referenced in the FigJam topology. All connectors were
repointed to the canonical references.

The preserved placeholder layers remain underneath the canonical references because automated
approval review rejected destructive deletion or hiding. They are no longer visible or targeted by
the topology connectors.

## Current evidence

### Onboarding

- FigJam page: `65:7716`
- topology Section: `65:6964`
- 30 visible application states
- 30 connectors
- all connector endpoints use `BOTTOM` → `TOP`
- 9 direct canonical instances
- 19 references to existing canonical Figma masters
- 2 references to new editable proposed masters
- Google sign-in master: Figma `1762:7749`
- Activation master: Figma `1762:7805`
- 21 preserved placeholder frames are fully covered and receive no connector endpoints

This proves the current Onboarding map is structurally complete and every visible state has a
canonical or explicitly proposed editable master. It does not prove that Samuel has approved every
product decision or that the screens have been implemented in RemClaw.

### Settings

- FigJam page: `0:1`
- topology Section: `9:49`
- 48 visible topology nodes
- 47 orthogonal `BOTTOM` → `TOP` connectors
- 30 existing editable Rem instances
- 12 Models reference occurrences whose canonical designs remain in Figma
- 6 explicit product gaps: connector discovery, connector detail, connected accounts, permission
  policy, tool overrides, and successful key removal
- 32 hidden legacy screen groups and 31 hidden legacy connectors remain preserved outside the visible
  topology; no visible connector targets a hidden node

This proves the Settings graph has been mapped and its gaps have been made visible. It does not prove
Settings screen-design completion.

### Other lanes

#### Competitive Audits

- FigJam page: `87:9669`
- one research-only page
- four source-app Sections: Muse, Claude, ChatGPT, and Cursor
- 18 image-backed audit references: 4 Muse, 4 Claude, 5 ChatGPT, and 5 Cursor
- no Rem product screens or cross-app wrapper Section

This proves the requested one-page organization and evidence count. Image-fill presence alone does
not prove the original capture URL, timestamp, or live product state; those claims require the
source record for each audit image.

#### Agenda

- FigJam page: `65:7717`
- topology Section: `65:7372`
- 43 full-screen states
- 40 connectors, all structurally `BOTTOM` → `TOP`
- 22 direct instances of remote canonical Figma screen components
- 21 full-screen states composed inside FigJam from remote canonical components

The 21 composed states are real, editable component compositions rather than flattened or hand-drawn
facsimiles. They still lack Figma Design screen masters, so Agenda can pass topology structure while
remaining screen-design-incomplete. Samuel's product and hierarchy review is also pending.

#### Chat

- FigJam page: `65:7712`
- topology Section: `65:7678`
- 59 full-screen state groups, including the Daily Brief branch
- 52 connectors including the nested Daily Brief branch; all are structurally `BOTTOM` → `TOP`
- 1 direct instance of a remote canonical Figma screen component
- 58 full-screen states composed inside FigJam from remote canonical components

The Chat states use real remote components, including the platform status bar, Chat toolbar,
MessageBubble, RemComposerBar, VoiceBar, and specialized browser or action surfaces. However, their
screen compositions live on the FigJam board rather than as Figma Design masters. Chat is therefore
topology evidence, not finished canonical screen design. Samuel's visual hierarchy and product-flow
review remains pending.

## Workflow correction

The durable workflow now uses one persistent chat per lane: Settings, Onboarding, Agenda, Chat, and
Competitive Audits. The root coordinator owns the program goal and routes Samuel's feedback. Lane
agents receive bounded assignments and return to idle after delivering a compact evidence packet.
They do not create a new chat for every correction.

Each packet contains exact Figma IDs, changed screens and provenance, screen and connector counts,
the latest overview and necessary closeups, unresolved product decisions, explicit gaps, and one
bounded next action.

Review is also bounded:

1. The Builder checks structure and rendered output, then submits one evidence packet.
2. The Reviewer performs one full pass against the explicit contract.
3. The Builder repairs evidenced blockers and submits a delta packet.
4. The Reviewer checks only changed nodes and directly affected transitions.
5. Remaining product or taste decisions go to Samuel; access and evidence failures go to the
   Steward. The Builder and Reviewer do not continue an open-ended feedback loop.

Every result now carries separate verdicts for topology completion, screen-design completion, and
research completion. A passing graph cannot conceal a fake or unverified screen.

## Reusable product-design rules learned

- Show full application-screen or modal states in topology; never substitute isolated components.
- Use the Settings hierarchy as the map grammar: parent above children, siblings on one row, short
  orthogonal connectors, consistent scale, and no visible wrapper box around each screen.
- Stable surfaces with many modes, such as Chat, use scenario/state branches rather than a fake
  linear navigation flow.
- Start from the real entry point. If no first screen exists, use a compact app-icon entry marker.
- Preserve legacy sources in place while rebuilding; do not bring visible migration metadata into
  the new canonical topology.
- Report provenance for every completed screen. SVG reconstructions, flattened screenshots, blank
  frames, and ad hoc imitations do not pass as finished design-system screens.
- Keep design and implementation as separately authorized phases.

## Claims the article must not make yet

- That all four product topologies are screen-design complete.
- That Agent Factory autonomously made the product decisions.
- That the review system caught the fake Onboarding screens before Samuel did.
- That the Figma designs have been implemented in RemClaw.
- That the current process has demonstrated high first-pass fidelity across multiple independent
  screens.
- That the low-memory workflow has been measured over enough repeated runs to establish a reliable
  quantitative improvement.

## Evidence still needed before publication

- Samuel's current feedback and approval status for Settings, Onboarding, Agenda, and Chat.
- Final provenance audits for Agenda and Chat.
- The exact publication brief and examples from Nisha's email/PDFs.
- Any article-specific screenshots chosen from the final Figma and FigJam artifacts.
- A decision on whether the article foregrounds Agent Factory itself or the Factory Analyst process.
