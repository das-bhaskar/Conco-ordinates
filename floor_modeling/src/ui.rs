use bevy::{
    prelude::*,
    window::PrimaryWindow,
};
use bevy_egui::{
    EguiContexts, 
    egui,
};

use crate::state::{EditorMode, EditorState};

pub fn ui_system(
    mut contexts: EguiContexts,
    window: Single<&mut Window, With<PrimaryWindow>>,
    mut editor: ResMut<EditorState>,
) -> Result {
    let ctx = contexts.ctx_mut()?;

    let mut left = egui::SidePanel::left("left_panel")
        .resizable(true)
        .show(ctx, |ui| {
            ui.heading("Tools");
            ui.separator();

            ui.radio_value(&mut editor.mode, EditorMode::Draw, "Draw");
            ui.radio_value(&mut editor.mode, EditorMode::Select, "Select");
            ui.radio_value(&mut editor.mode, EditorMode::MoveSelected, "Move Selected");

            ui.allocate_rect(ui.available_rect_before_wrap(), egui::Sense::hover());
        })
        .response
        .rect
        .width();

    left *= window.scale_factor();

    editor.ui_hovered = ctx.is_pointer_over_area();

    Ok(())
}