<!-- category: Fixed -->
- **Demo app: 3D Scan floor plan no longer shrinks a room to an interior partition.** A short wall in the middle of the room used to be read as the room's edge; only walls on the floor's edge, or spanning most of a side, now bound the room.
- **Demo app: a very large 3D Scan opens its floor plan without running out of memory.** The survey grid is capped at 48 MiB; past that the survey reads "not measured" instead of crashing.
