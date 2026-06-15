create AI Agents files for `Spec-driven development` Initial set-up , which includes the creating following files in `.github` folder like we have did in this current #codebase

- design.md
- requirements.md
- tasks.md


---


For `requirements.md` file, follow this

```md

# Requirements Document

## Introduction


## Requirements

### Requirement 1: User Registration & Authentication
**User Story:** 

#### Acceptance Criteria

### Requirement 2: {...}
**User Story:**

#### Acceptance Criteria

```










---


For `design.md` file, don't include code /sample snippet in it, use <context_mapping>. Here is the structure of design.md


```md

# {Project_Name} — Design & Architecture

## System Overview
---

## Tech Stack
---

## Architecture
---

## Core Modules
---
## Project Structure
---

## Future Enhancements

```






---

For `tasks.md` file, 


```md

# Implementation Plan

- [x] 1. Set up project structure and core interfaces
  - Create directory structure for models, services, attack engines, and UI components
  - Define base interfaces and abstract classes for forensics operations
  - Set up configuration management for tool paths and settings
  - _Requirements: 2.1, 2.2

- [-] 2. Implement core data models and validation
  - [x] 2.1 Create AndroidDevice model with validation
    - Add validation methods for device metadata integrity

```





<guiding_rules>
Do not mention `spec-driven development` apart from the Front matter and Description of the skill file.

</guiding_rules>



<context_mapping>
Follow the below techniques for mapping the code snippet in the design file, 

**For a single line:**
```md
[code](../src/api.ts#L23)
```

**For a range of lines:**
```md
[code](../src/api.ts#L23-L30)
```

</context_mapping>


---


How we gonna create the files, whether using skill file or prompting copilot agent to create these files using VS code build-in file creation tools or using scripts (ps1 or cmd or .js). Tell me which one will be optimal. 

if `/.github` folder exists, create spec-files in it. Otherwise create `/.github` folder and create spec-files in it.



---

The question is 






<spec_driven_development>


Requirements Gathering -> Design file -> Implementation


1. Requirements Gathering

If a User come up with an idea of building an application /product, ask a series of questions (with Options that are Optimal and reliable for that application) -until you've have gathered enough context for building that application.

For example: 

`User: Build an task tracker`

the user didn't mentioned, 

- what Frontend technology has to be used? {React, Angular, ...}
- what Backend has to be used? {Springboot, ...}
- {...}


(Mention Specific version, if you've in mind)

---

2. Design document

For `design.md` file, don't include code /sample snippet in it, use <context_mapping>. Here is the structure of design.md



3. Implementation

The Agent should always tracking the Status of tasks, marks as follows in the square bracket `[]`,
- To do tasks ` ` (Empty space)
- In progress `-` (Hyphen)
- Completed `✓` 


</spec_driven_development>
