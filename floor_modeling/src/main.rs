mod state;
mod grid;
mod camera;
mod wall_ops;
mod ui;

use bevy::{
    prelude::*,
};
use bevy_egui::{
    EguiPlugin, 
    EguiPrimaryContextPass,
};

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
        .init_resource::<state::WallSettings>()
        .init_resource::<state::EditorSettings>()
        .add_systems(Startup, camera::setup_camera)
        .add_systems(EguiPrimaryContextPass, ui::ui_system)
        .add_systems(
            Update,
            (
                camera::camera_movement,
                wall_ops::draw_wall,
                wall_ops::delete_selected_walls,
                wall_ops::wall_selection,
            ),
        )
        .add_systems(
            PostUpdate,
            (grid::draw_grid, 
                wall_ops::render_walls, 
            ),
        )
        .run();
}