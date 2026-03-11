use bevy::prelude::*;
use bevy_egui::EguiContext;

use crate::state::GridSettings;

pub fn draw_grid(
    mut gizmos: Gizmos,
    settings: Res<GridSettings>,
    camera_query: Single<(&GlobalTransform, &Projection), (With<Camera2d>, Without<EguiContext>)>,
    window: Single<&Window>,
) {
    if !settings.visible {
        return;
    }

    let (global_transform, projection) = camera_query.into_inner();

    let scale = match projection {
        Projection::Orthographic(ortho) => ortho.scale,
        _ => panic!("Camera projection was not orthographic. This shouldn't be possible."),
    };

    let size = settings.cell_size;
    if size < 1.0 {
        return;
    }

    let half_w = window.width() * 0.5 * scale;
    let half_h = window.height() * 0.5 * scale;
    let camera_pos = global_transform.translation().truncate();

    let min_x = camera_pos.x - half_w;
    let max_x = camera_pos.x + half_w;
    let min_y = camera_pos.y - half_h;
    let max_y = camera_pos.y + half_h;

    let start_x = (min_x / size).floor() as i32;
    let end_x = (max_x / size).ceil() as i32;
    let start_y = (min_y / size).floor() as i32;
    let end_y = (max_y / size).ceil() as i32;

    for i in start_x..=end_x {
        let x = i as f32 * size;
        gizmos.line_2d(Vec2::new(x, min_y), Vec2::new(x, max_y), settings.color);
    }

    for i in start_y..=end_y {
        let y = i as f32 * size;
        gizmos.line_2d(Vec2::new(min_x, y), Vec2::new(max_x, y), settings.color);
    }

    gizmos.line_2d(
        Vec2::new(min_x, 0.0),
        Vec2::new(max_x, 0.0),
        settings.origin_color,
    );
    gizmos.line_2d(
        Vec2::new(0.0, min_y),
        Vec2::new(0.0, max_y),
        settings.origin_color,
    );
}
