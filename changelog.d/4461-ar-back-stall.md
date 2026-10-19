<!-- category: Fixed -->
- **Demo app**: Back from an AR demo no longer redraws the AR launcher as "Checking…" and asks ARCore again inside the exit animation. The launcher comes back with the answer it already had. The ARCore check now runs after the 350 ms transition, so it no longer adds main-thread work to the transition's first frames (#4461).
