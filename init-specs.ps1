$targetDir = Join-Path (Get-Location) ".github"

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir | Out-Null
    Write-Host "Created .github directory." -ForegroundColor Green
}

$reqPath = Join-Path $targetDir "requirements.md"
$designPath = Join-Path $targetDir "design.md"
$tasksPath = Join-Path $targetDir "tasks.md"

$reqContent = @"
# Requirements Document

## Introduction

## Requirements

### Requirement 1: [Feature Name]
**User Story:** 

#### Acceptance Criteria

### Requirement 2: [...]
**User Story:**

#### Acceptance Criteria
"@

$designContent = @"
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
"@

$tasksContent = @"
# Implementation Plan

- [ ] 1. Set up project structure and core interfaces
  - Create directory structure
  - Define base interfaces and abstract classes
  - Set up configuration management
  - _Requirements: 1, 2

- [ ] 2. Implement core data models and validation
  - [ ] 2.1 [Sub-task]
"@

Set-Content -Path $reqPath -Value $reqContent -Encoding UTF8 -Force
Set-Content -Path $designPath -Value $designContent -Encoding UTF8 -Force
Set-Content -Path $tasksPath -Value $tasksContent -Encoding UTF8 -Force

Write-Host "Spec-driven development files successfully initialized in .github/" -ForegroundColor Green
