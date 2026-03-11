mod camera;
mod graph;
mod grid;
mod state;
mod ui;

use bevy::prelude::*;
use bevy_egui::{EguiPlugin, EguiPrimaryContextPass};

use crate::state::{Graph, SelectedEdges, SelectedVertices};

fn main() {
    App::new()
        .add_plugins(DefaultPlugins.set(WindowPlugin {
            primary_window: Some(Window {
                title: "Floorplan Editor".to_string(),
                resolution: bevy::window::WindowResolution::new(1280, 720),
                ..default()
            }),
            ..default()
        }))
        .add_plugins(EguiPlugin::default())
        .init_resource::<state::EditorState>()
        .init_resource::<state::GridSettings>()
        .init_resource::<state::GraphSettings>()
        .init_resource::<state::EditorSettings>()
        .init_resource::<Graph>()
        .init_resource::<SelectedVertices>()
        .init_resource::<SelectedEdges>()
        .add_systems(Startup, camera::setup_camera)
        .add_systems(EguiPrimaryContextPass, ui::ui_system)
        .add_systems(
            Update,
            (
                camera::camera_movement,
                graph::graph_operations,
            ),
        )
        .add_systems(PostUpdate, (grid::draw_grid, graph::render_graph.after(graph::graph_operations)))
        .run();
}
