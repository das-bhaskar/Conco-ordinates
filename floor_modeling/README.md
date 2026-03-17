## Summary

Application for editing floorplans using [Bevy](https://bevy.org). The goal is to be a starting point for allowing us to manually translate the horrible floorplans we have been given to something that can be converted into a simple weighted graph that can be navigated automatically.

Current editor features include:

- Drawing vertices/edges
- Deleting vertices/edges
- Moving the camera
- Saving/Loading
- Assigning labels
- Undo/Redo shortcuts using event sourcing

## Requirements To Build

Install [Rust](https://rust-lang.org/learn/get-started/)

## Building

To make a debug build

```
cargo build
```

To make a release build

```
cargo build --release
```

## Running Executable

The following commands will also build the executable if not already done.

For a debug build

```
cargo run
```

For a release build

```
cargo run --release
```
