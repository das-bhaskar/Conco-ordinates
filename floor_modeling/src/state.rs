use bevy::prelude::*;


#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum EditorMode {
    #[default]
    Draw,
    Select,
    MoveSelected,
}

#[derive(Resource)]
pub struct EditorSettings {
    pub camera_wasd_speed: f32,
    pub select_threshold: f32,
}

impl Default for EditorSettings {
    fn default() -> Self {
        Self {
            camera_wasd_speed: 500.0,
            select_threshold: 10.0,
        }
    }
}

#[derive(Resource)]
pub struct EditorState {
    pub mode: EditorMode,
    pub is_drawing: bool,
    pub draw_start: Option<Vec2>,
    pub draw_current: Option<Vec2>,
    pub ui_hovered: bool,
}

impl Default for EditorState {
    fn default() -> Self {
        Self {
            mode: EditorMode::Draw,
            is_drawing: false,
            draw_start: None,
            draw_current: None,
            ui_hovered: false,
        }
    }
}

#[derive(Resource)]
pub struct WallSettings {
    pub color: Color,
    pub selected_color: Color,
    pub drawing_color: Color,
}

impl Default for WallSettings {
    fn default() -> Self {
        Self {
            color: Color::srgb(0.9, 0.9, 0.9),
            selected_color: Color::srgb(0.2, 0.6, 1.0),
            drawing_color: Color::srgba(1.0, 1.0, 0.0, 0.7),
        }
    }
}

#[derive(Component)]
pub struct Wall {
    pub start: Vec2,
    pub end: Vec2,
}

impl Wall {
    pub fn new(start: Vec2, end: Vec2) -> Self {
        Wall { start, end }
    }
}

#[derive(Component)]
pub struct SelectedWalls;

#[derive(Resource)]
pub struct GridSettings {
    pub cell_size: f32,
    pub visible: bool,
    pub color: Color,
    pub origin_color: Color,
}

impl Default for GridSettings {
    fn default() -> Self {
        Self {
            cell_size: 50.0,
            visible: true,
            color: Color::srgba(0.3, 0.3, 0.3, 0.4),
            origin_color: Color::srgba(0.6, 0.6, 0.0, 0.6),
        }
    }
}