use bevy::prelude::*;
use bevy_egui::EguiContext;

use crate::state::{EditorMode, EditorSettings, EditorState, SelectedWalls, Wall, WallSettings};

pub fn draw_wall(
    mut commands: Commands,
    mut editor: ResMut<EditorState>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    camera_query: Single<(&Camera, &GlobalTransform), (With<Camera2d>, Without<EguiContext>)>,
    window: Single<&Window>,
) {
    match editor.mode {
        EditorMode::Draw => {

        },
        _ => {
            return;
        }
    }
    
    let (camera, global_transform) = camera_query.into_inner();

    let cursor_pos = 
    match window.cursor_position() {
        Some(cursor_pos) => cursor_pos,
        None => {
            return;
        }
    };

    let cursor_world_pos = 
    match camera.viewport_to_world_2d(global_transform, cursor_pos) {
        Ok(cursor_world_pos) => cursor_world_pos,
        Err(_) => {
            return;
        }
    };

    if mouse_button.just_pressed(MouseButton::Left) && !editor.ui_hovered {
        editor.is_drawing = true;
        editor.draw_start = Some(cursor_world_pos);
    }

    if editor.is_drawing {
        editor.draw_current = Some(cursor_world_pos);
    }

    if mouse_button.just_released(MouseButton::Left) && editor.is_drawing {
        if let Some(start) = editor.draw_start {
            let end = cursor_world_pos;
            if start.distance(end) > 2.0 {
                commands.spawn(Wall::new(start, end));
            }
        }
        editor.is_drawing = false;
        editor.draw_start = None;
        editor.draw_current = None;
    }
}

pub fn render_walls(
    mut gizmos: Gizmos,
    walls: Query<(&Wall, Option<&SelectedWalls>)>,
    settings: Res<WallSettings>,
    editor: Res<EditorState>,
) {
    for (wall, selected) in walls.iter() {
        let color = if selected.is_some() {
            settings.selected_color
        } else {
            settings.color
        };
        gizmos.line_2d(wall.start, wall.end, color);
        gizmos.circle_2d(wall.start, 3.0, color);
        gizmos.circle_2d(wall.end, 3.0, color);
    }

    if editor.is_drawing {
        if let (Some(start), Some(current)) = (editor.draw_start, editor.draw_current) {
            gizmos.line_2d(start, current, settings.drawing_color);
            gizmos.circle_2d(start, 3.0, settings.drawing_color);
            gizmos.circle_2d(current, 3.0, settings.drawing_color);
        }
    }
}

pub fn wall_selection(
    mut commands: Commands,
    editor: Res<EditorState>,
    editor_settings: Res<EditorSettings>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    camera_query: Single<(&Camera, &GlobalTransform), (With<Camera2d>, Without<EguiContext>)>,
    window: Single<&Window>,
    walls: Query<(Entity, &Wall, Option<&SelectedWalls>)>,
) {
    if editor.mode != EditorMode::Select || editor.ui_hovered {
        return;
    }

    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    }

    let (camera, global_transform) = *camera_query;

    let cursor_pos = 
    match window.cursor_position() {
        Some(cursor_pos) => cursor_pos,
        None => {
            return;
        }
    };

    let cursor_world_pos = 
    match camera.viewport_to_world_2d(global_transform, cursor_pos) {
        Ok(cursor_world_pos) => cursor_world_pos,
        Err(_) => {
            return;
        }
    };

    let shift_held = keyboard.pressed(KeyCode::ShiftLeft);

    let mut closest: Option<(Entity, f32, bool)> = None;
    for (entity, wall, selected) in walls.iter() {
        let dist = dist_from_line(cursor_world_pos, wall.start, wall.end);
        if dist < editor_settings.select_threshold {
            if closest.is_none() || dist < closest.unwrap().1 {
                closest = Some((entity, dist, selected.is_some()));
            }
        }
    }

    match closest {
        Some((entity, _, already_selected)) => {
            if shift_held {
                if already_selected {
                    commands.entity(entity).remove::<SelectedWalls>();
                } else {
                    commands.entity(entity).insert(SelectedWalls);
                }
            } else {
                for (e, _, selected) in walls.iter() {
                    if selected.is_some() {
                        commands.entity(e).remove::<SelectedWalls>();
                    }
                }
                if !already_selected {
                    commands.entity(entity).insert(SelectedWalls);
                }
            }
        },
        None => {
            for (e, _, selected) in walls.iter() {
                if selected.is_some() {
                    commands.entity(e).remove::<SelectedWalls>();
                }
            }
        }
    }
}

pub fn delete_selected_walls(
    mut commands: Commands,
    keyboard: Res<ButtonInput<KeyCode>>,
    selected_walls: Query<Entity, (With<Wall>, With<SelectedWalls>)>,
) {
    if keyboard.just_pressed(KeyCode::Delete) {
        for entity in selected_walls.iter() {
            commands.entity(entity).despawn();
        }
    }
}

fn dist_from_line(point: Vec2, a: Vec2, b: Vec2) -> f32 {
    let ab = b - a;
    let ap = point - a;
    let len_sq = ab.length_squared();
    if len_sq < f32::EPSILON {
        return ap.length();
    }
    let t = (ap.dot(ab) / len_sq).clamp(0.0, 1.0);
    let projection = a + ab * t;
    (point - projection).length()
}
