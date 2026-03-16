use bevy::{prelude::*, window::PrimaryWindow};
use bevy_egui::{EguiContext, EguiContexts, egui::{self, Color32}};
use bevy::color::Srgba;

use crate::state::{
    BackgroundImageSettings, 
    EditorState, 
    Graph, 
    GraphSettings, 
    GridSettings,
    SelectedEdges, 
    SelectedVertices,
};

pub fn ui_system(
    mut contexts: EguiContexts,
    window: Single<&mut Window, With<PrimaryWindow>>,
    mut editor: ResMut<EditorState>,
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut graph_settings: ResMut<GraphSettings>,
    mut graph: ResMut<Graph>,
    mut selected_vertices: ResMut<SelectedVertices>,
    mut selected_edges: ResMut<SelectedEdges>,
    mut grid_settings: ResMut<GridSettings>,
) -> Result {
    let ctx = contexts.ctx_mut()?;

    let mut left = egui::SidePanel::left("left_panel")
        .resizable(true)
        .show(ctx, |ui| {
            ui.heading("Tools");
            ui.separator();

            ui.heading("Background Image");

            if ui.button("Import Image...").clicked() {
                bg_settings.input_path = match rfd::FileDialog::new()
                    .add_filter("Images", &["png", "jpg", "svg"])
                    .pick_file()
                {
                    Some(path) => Some(path.display().to_string()),
                    None => None,
                };
            }

            if let Some(ref path) = bg_settings.image_path {
                let file_name = std::path::Path::new(path)
                    .file_name()
                    .and_then(|n| n.to_str())
                    .unwrap_or("Unknown");
                ui.label(format!("File: {}", file_name));
            }

            ui.add(
                egui::Slider::new(&mut bg_settings.width_meters, 1.0..=1000.0).text("Width (m)"),
            );
            ui.add(egui::Slider::new(&mut bg_settings.opacity, 0.0..=1.0).text("Opacity"));
            ui.checkbox(&mut bg_settings.visible, "Visible");

            ui.separator();

            ui.heading("Graph Colors");

            color_picker(ui, "Vertex", &mut graph_settings.vertex_color);
            color_picker(ui, "Edge", &mut graph_settings.edge_color);
            color_picker(ui, "Selected", &mut graph_settings.selected_color);
            color_picker(ui, "Edge Drawing", &mut graph_settings.edge_drawing_color);

            ui.separator();

            ui.heading("File");

            if ui.button("Save Project...").clicked() {
                if let Some(path) = rfd::FileDialog::new()
                    .add_filter("Floor Plan", &["json"])
                    .save_file()
                {
                    if let Err(e) = crate::state::save_to_file(
                        &path.display().to_string(),
                        &graph,
                        &selected_vertices,
                        &selected_edges,
                        &graph_settings,
                        &grid_settings,
                        &bg_settings,
                    ) {
                        eprintln!("Failed to save project: {}", e);
                    }
                }
            }

            if ui.button("Load Project...").clicked() {
                if let Some(path) = rfd::FileDialog::new()
                    .add_filter("Floor Plan", &["json"])
                    .pick_file()
                {
                    match crate::state::load_from_file(&path.display().to_string()) {
                        Ok(data) => {
                            *graph = data.graph;
                            *selected_vertices = data.selected_vertices;
                            *selected_edges = data.selected_edges;
                            *graph_settings = data.graph_settings;
                            *grid_settings = data.grid_settings;
                            let reload_path = data.background_settings.image_path.clone();
                            *bg_settings = data.background_settings;
                            if let Some(p) = reload_path {
                                bg_settings.input_path = Some(p);
                            }
                        }
                        Err(e) => eprintln!("Failed to load project: {}", e),
                    }
                }
            }

            ui.separator();

            if !selected_vertices.indices.is_empty() || !selected_edges.indices.is_empty() {
                ui.heading("Selection");

                if selected_vertices.indices.len() == 1 {
                    let index = *selected_vertices.indices.iter().next().unwrap();
                    let vertex = &mut graph.graph[index];
                    let label_color = ui.visuals().text_color();
                    ui.label(egui::RichText::new("Vertex Labels").color(label_color));
                    let mut remove_idx = None;
                    for (i, label) in vertex.labels.iter_mut().enumerate() {
                        ui.horizontal(|ui| {
                            ui.text_edit_singleline(label);
                            if ui.small_button("X").clicked() {
                                remove_idx = Some(i);
                            }
                        });
                    }
                    if let Some(i) = remove_idx {
                        vertex.labels.remove(i);
                    }
                    if ui.small_button("+ Add Label").clicked() {
                        vertex.labels.push(String::new());
                    }
                } else if selected_vertices.indices.len() > 1 {
                    ui.label(format!("{} vertices selected", selected_vertices.indices.len()));
                }

                if selected_edges.indices.len() == 1 {
                    let index = *selected_edges.indices.iter().next().unwrap();
                    let edge = &mut graph.graph[index];
                    let label_color = ui.visuals().text_color();
                    ui.label(egui::RichText::new("Edge Labels").color(label_color));
                    let mut remove_idx = None;
                    for (i, label) in edge.labels.iter_mut().enumerate() {
                        ui.horizontal(|ui| {
                            ui.text_edit_singleline(label);
                            if ui.small_button("X").clicked() {
                                remove_idx = Some(i);
                            }
                        });
                    }
                    if let Some(i) = remove_idx {
                        edge.labels.remove(i);
                    }
                    if ui.small_button("+ Add Label").clicked() {
                        edge.labels.push(String::new());
                    }
                } else if selected_edges.indices.len() > 1 {
                    ui.label(format!("{} edges selected", selected_edges.indices.len()));
                }

                ui.separator();
            }

            ui.allocate_rect(ui.available_rect_before_wrap(), egui::Sense::hover());
        })
        .response
        .rect
        .width();

    left *= window.scale_factor();

    editor.ui_hovered = ctx.is_pointer_over_area();

    Ok(())
}

pub fn render_labels(
    mut contexts: EguiContexts,
    graph: Res<Graph>,
    settings: Res<GraphSettings>,
    camera_query: Single<(&Camera, &GlobalTransform), (With<Camera2d>, Without<EguiContext>)>,
) -> Result {
    let ctx = contexts.ctx_mut()?;
    let painter = ctx.layer_painter(egui::LayerId::background());
    let (camera, global_transform) = camera_query.into_inner();

    let font = egui::FontId::proportional(12.0);
    let line_height = 14.0;

    for index in graph.vertex_indices() {
        let vertex = graph.vertex(index);
        if vertex.labels.is_empty() {
            continue;
        }
        if let Ok(screen_pos) = camera.world_to_viewport(global_transform, vertex.transform.extend(0.0)) {
            let color = bevy_color_to_egui(settings.vertex_color);
            let base_y = screen_pos.y - 10.0;
            for (i, label) in vertex.labels.iter().rev().enumerate() {
                painter.text(
                    egui::pos2(screen_pos.x, base_y - i as f32 * line_height),
                    egui::Align2::CENTER_BOTTOM,
                    label,
                    font.clone(),
                    color,
                );
            }
        }
    }

    for index in graph.edge_indices() {
        let edge = graph.edge(index);
        if edge.labels.is_empty() {
            continue;
        }
        let (left, right) = graph.edge_vertex_pair(index);
        let midpoint = (left.transform + right.transform) / 2.0;
        if let Ok(screen_pos) = camera.world_to_viewport(global_transform, midpoint.extend(0.0)) {
            let color = bevy_color_to_egui(settings.edge_color);
            let base_y = screen_pos.y - 10.0;
            for (i, label) in edge.labels.iter().rev().enumerate() {
                painter.text(
                    egui::pos2(screen_pos.x, base_y - i as f32 * line_height),
                    egui::Align2::CENTER_BOTTOM,
                    label,
                    font.clone(),
                    color,
                );
            }
        }
    }

    Ok(())
}

fn color_picker(ui: &mut egui::Ui, label: &str, color: &mut Color) {
    let srgba = Srgba::from(*color);
    let mut c32 = egui::Color32::from_rgba_unmultiplied(
        (srgba.red * 255.0) as u8,
        (srgba.green * 255.0) as u8,
        (srgba.blue * 255.0) as u8,
        (srgba.alpha * 255.0) as u8,
    );
    ui.horizontal(|ui| {
        egui::color_picker::color_edit_button_srgba(ui, &mut c32, egui::color_picker::Alpha::OnlyBlend);
        ui.label(label);
    });
    let [r, g, b, a] = c32.to_srgba_unmultiplied();
    *color = Color::srgba(
        r as f32 / 255.0,
        g as f32 / 255.0,
        b as f32 / 255.0,
        a as f32 / 255.0,
    );
}

fn bevy_color_to_egui(color: Color) -> Color32 {
    let srgba = Srgba::from(color);
    Color32::from_rgb(
        (srgba.red * 255.0) as u8,
        (srgba.green * 255.0) as u8,
        (srgba.blue * 255.0) as u8,
    )
}