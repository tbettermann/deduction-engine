# deduction-engine

A Kotlin-based logic engine for deduction games.  
Includes both an **evaluator** and a **simulator** to analyze deduction scenarios and simulate players' reasoning
processes.
Inspired by classic deduction board games like *Clue/Cluedo*.
---

## Features

- Define custom subjects, tools, and rooms
- Represent and evaluate possible combinations
- Run simulations with various strategies

---

## Example Entities

You can define your own card sets like:

```json
[
  { "type": "ROOM", "id": "room_1", "displayNames": { "en": "Room 1" } },
  { "type": "SUBJECT", "id": "subject_1", "displayNames": { "en": "Suspect A" } },
  { "type": "TOOL", "id": "tool_1", "displayNames": { "en": "Object X" } }
]
