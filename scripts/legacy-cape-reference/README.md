# Reproducing the 1.8.9 numerical reference

This development-only harness records rotations from the original cape renderer,
using minimal player and graphics stand-ins. Its tick inputs reproduce the
history operations verified in 1.8.9's player/entity bytecode. Numerical fixtures
cover idle, sprint, strafe, jump, fall, sneak, yaw wrapping, large forward/lateral
motion, teleportation, riding and airborne movement at five partial ticks.

Obtain the client from the `downloads.client` entry in Mojang's official metadata:
https://piston-meta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json

Require SHA-1 `3870888a6c3d349d3771a3e9d16c9bf5e076b908` before running the harness.
Use a temporary directory outside the source tree. Compile `Oracle.java` with
that client on the compiler classpath. Extract only `bkp.class` and `ns.class`
from the client into the temporary classes directory beside the compiled harness.
Run `java -cp <temporary-classes-directory> Oracle` to produce the CSV. Extracting
those two classes avoids mixing signed Minecraft classes and unsigned stand-ins
within the unnamed Java package. Do not modify or redistribute the original JAR.

Only the harness source and recorded numerical CSV belong in this repository;
reference binaries and compiled stand-ins must remain outside it. The main and
sources JARs do not include this harness or any test assets.
