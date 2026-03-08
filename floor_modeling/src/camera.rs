use bevy::prelude::*;
use bevy::camera::{CameraOutputMode, visibility::RenderLayers};
use bevy::input::mouse::{MouseMotion, MouseWheel};
use bevy_egui::{EguiContext, EguiGlobalSettings, PrimaryEguiContext};

use crate::state::{EditorSettings, EditorState};

pub fn setup_camera(
    mut commands: Commands,
    mut egui_global_settings: ResMut<EguiGlobalSettings>,
) {
    egui_global_settings.auto_create_primary_context = false;

    // World camera.
    commands.spawn((
        Camera2d,
        Camera::default(),
        Transform::from_translation(Vec3::new(0.0, 0.0, 999.0)),
    ));
    
    // Egui camera.
    commands.spawn((
        PrimaryEguiContext,
        Camera2d,
        RenderLayers::none(),
        Camera {
            order: 1,
            output_mode: CameraOutputMode::Write {
                blend_state: Some(bevy::render::render_resource::BlendState::ALPHA_BLENDING),
                clear_color: ClearColorConfig::None,
            },
            clear_color: ClearColorConfig::Custom(Color::NONE),
            ..default()
        },
    ));
}

pub fn camera_movement(
    camera_query: Single<(&mut Transform, &mut Projection), (With<Camera2d>, Without<EguiContext>)>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    editor: Res<EditorState>,
    editor_settings: Res<EditorSettings>,
    time: Res<Time<Fixed>>,
    mut mouse_motion: MessageReader<MouseMotion>,
    mut scroll_events: MessageReader<MouseWheel>,
) {
    let (mut transform, mut projection) = camera_query.into_inner();

    let (ortho, scale) = match &mut *projection {
        Projection::Orthographic(ortho) => {
            let scale = ortho.scale;
            (ortho, scale)
        }
        _ => panic!("Camera projection was not orthographic. This shouldn't be possible.")
    };

    // Pan with right mouse button or wasd
    if mouse_button.pressed(MouseButton::Right) {
        for ev in mouse_motion.read() {
            transform.translation.x -= ev.delta.x * scale;
            transform.translation.y += ev.delta.y * scale;
        }
    } else {
        if keyboard.pressed(KeyCode::KeyW) {
            transform.translation.y += editor_settings.camera_wasd_speed * time.delta_secs();
        }
        if keyboard.pressed(KeyCode::KeyS) {
            transform.translation.y -= editor_settings.camera_wasd_speed * time.delta_secs();
        }
        if keyboard.pressed(KeyCode::KeyA) {
            transform.translation.x -= editor_settings.camera_wasd_speed * time.delta_secs();
        }
        if keyboard.pressed(KeyCode::KeyD) {
            transform.translation.x += editor_settings.camera_wasd_speed * time.delta_secs();
        }
    }

    // Zoom with scroll wheel
    for ev in scroll_events.read() {
        if editor.ui_hovered {
            continue;
        }

        let zoom_factor = 1.0 - ev.y * 0.1;
        ortho.scale = (ortho.scale * zoom_factor).clamp(0.05, 50.0);
    }
}