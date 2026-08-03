How could we possible achieve Adobe Acrobat-level annotations (high-fidelity rendering, seamless ink drawing, standard PDF object highlighting, standard annotation flattening)



The annotations are stored in a mobile temporary storage and it's over writing onto another pdf. 

Read Aloud: 
- the highlighting area while reading isn't aligning with the PDF container. the highlighting area is floating some where else
- It should directly embed those annotations and highlights directly into the pdf. How do we achieve it?





- [X] 4. Build Core Reader UI (Jetpack Compose, gestures, paging, zoom)
- [X] 5. Implement Google Drive Sync via Storage Access Framework (Directly open and sync PDFs)
- [ ] 6. Implement Highlighting & Annotation Features (Customizable colors, Eraser tools)
- [ ] 7. Implement PDFBox Integration (Embedding annotations back to the PDF file)
- [ ] 8. Implement Read Aloud (Parsing PDF text, native Android TTS, playback state)
- [ ] 9. Refine Performance & Latency (Optimize memory allocations, caching rendered bitmaps)


# Feature blueprints
 
- the agent-optimized blueprint structure



# NoxReader

- Table of Contents (Top left corner)

- 

## Text to Speech features

- Back to the previous paragraph

- Fast forward to next paragraph

- Speed selection

- Auto page scroll (option)



the read aloud isn't syncing with the line text highlight.
<!-- - Black out other content than the  -->

## Home UI

- Add to Favorites

- Add to read

- Add to have read

- Collections

- More options (Move to trash, Edit (Title, Author, Series, Annotation))

## Annotations

After saving, the width of the annotation is increased drastically

- Put annotation color setting in the `settings`
- Annotation color settings UI (in the PDF reading window) is so worst, I can't even a drag it down. [Don't repeat this]







Accorlite 

DB




>










issues:
---

use context7 mcp to fetch relevant documents






what does agent specs even means?

---

Each file should is a blueprint for implementing every a one feature (feature -> ie. Highlighter or an annotation marker or anything...) not combining behavior of a feature with user interaction ( i.e. existing-highlight-selection.md)

---

Every feature file must contains every behavior, user interaction and cases with in that file only, 

for eg, if we gonna implement highlighter feature, it includes
- selecting the text 
- Highlighting the selected area
- flattening the highlighted area

[Note: it's just an example, not actual feature behavior]

---------------






feature-blueprints/ to contain only user-facing feature specifications, with all shared technical contracts kept in .github/architecture.md

--------------



come up with an optimal way for structuring the feature file




@skill-creator create a skill that tells the AI Agent on how to update/ create -> `feature-blueprints` and `architecture.md` .

add examples in the references folder of the skill (or where ever it's has to be placed)


[Don't build the skill with the current project specific details]













use DataStore Proto with schema migrations capabilities


Archive or remove stale implementation-files/ documents. Several contain obsolete file:///x:/... links and duplicate current documentation.




After those foundations, the strongest product additions would be document search, outline navigation, thumbnails/scrubber, undo/redo, automatic draft recovery, “Save a copy” for read-only providers, and editing existing ink and notes.
My recommended next milestone is Safe Save and Recovery: fix SAF success detection, preserve flattened note content and ink width, guard unsaved navigation, serialize document operations, and add PDF/provider failure fixtures. No files were changed during this audit.




---

## Tool bar 


- tool bar icon doesn't  look good (assets\tool-bar.jpg)


- Annotation and highlighter ain't instantly marking up in the PDF. Its only imprinting after taking the hand off the screen (assets\annotation-boxy-appearance.jpg)


- And the annotation is increasing the width of the ink, after saving it. But when drawing its thinner


- I can't able to switch color, when I enter into Annotation or Highlighter


- After drawing, the annotations isn't smooth at all, it's kinda boxy (assets\annotation-boxy-appearance.jpg)


- And I can't able to reselect or delete the Annotations Ink after saving it




Zoom in and lock layout - pdf specific feature
