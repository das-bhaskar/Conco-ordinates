use bevy::{prelude::*, window::PrimaryWindow};
use bevy_egui::{EguiContext, EguiContexts, egui::{self, Color32}};
use bevy::color::Srgba;

use crate::{events::EventQueue, state::{
    BackgroundImageSettings, EditorSettings, EditorState, Graph, GraphSettings, GridSettings, SelectedEdges, SelectedVertices
}};

pub fn ui_system(
    mut contexts: EguiContexts,
    window: Single<&mut Window, With<PrimaryWindow>>,
    mut editor: ResMut<EditorState>,
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut graph_settings: ResMut<GraphSettings>,
    mut graph: ResMut<Graph>,
    mut event_queue: ResMut<EventQueue>,
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

            let mut width = bg_settings.width_meters;

            let response = ui.add(
                egui::Slider::new(&mut width, 1.0..=3000.0).text("Width (m)"),
            );
            if response.changed() {
                event_queue.push(crate::events::Event::SetBackgroundWidth(width));
            }
            ui.add(egui::Slider::new(&mut bg_settings.opacity, 0.0..=1.0).text("Opacity"));

            ui.separator();

            ui.heading("Graph Colors");

            if let Some(c) = color_picker(ui, "Vertex", &mut graph_settings.vertex_color) {
                event_queue.push(crate::events::Event::SetVertexColors(c));
            }
            if let Some(c) = color_picker(ui, "Edge", &mut graph_settings.edge_color) {
                event_queue.push(crate::events::Event::SetEdgeColors(c));
            }
            if let Some(c) = color_picker(ui, "Selected", &mut graph_settings.selected_color) {
                event_queue.push(crate::events::Event::SetSelectedColors(c));
            }
            if let Some(c) = color_picker(ui, "Edge Drawing", &mut graph_settings.edge_drawing_color) {
                event_queue.push(crate::events::Event::SetDrawingColors(c));
            }

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
                        &event_queue,
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
                            *event_queue = data.event_queue;
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
                    for (i, label) in vertex.labels.iter_mut().enumerate() {
                        ui.horizontal(|ui| {
                            let text_response = ui.text_edit_singleline(label);
                            if text_response.lost_focus() {
                                event_queue.push(crate::events::Event::UpdateVertexLabel(index, i, label.clone()));
                            }
                            if ui.small_button("X").clicked() {
                                event_queue.push(crate::events::Event::RemoveVertexLabel(index, i));
                            }
                            if ui.small_button("+").clicked() {
                                event_queue.push(crate::events::Event::AddVertexLabel(index, i));
                            }
                        });
                    }
                    if ui.small_button("+ Add Label").clicked() {
                        let vertex = &mut graph.graph[index];
                        event_queue.push(crate::events::Event::AddVertexLabel(index, vertex.labels.len()));
                    }
                } else if selected_vertices.indices.len() > 1 {
                    ui.label(format!("{} vertices selected", selected_vertices.indices.len()));
                }

                if selected_edges.indices.len() == 1 {
                    let index = *selected_edges.indices.iter().next().unwrap();
                    let edge = &mut graph.graph[index];
                    let label_color = ui.visuals().text_color();
                    ui.label(egui::RichText::new("Edge Labels").color(label_color));
                    for (i, label) in edge.labels.iter_mut().enumerate() {
                        ui.horizontal(|ui| {
                            let text_response = ui.text_edit_singleline(label);
                            if text_response.lost_focus() {
                                event_queue.push(crate::events::Event::UpdateEdgeLabel(index, i, label.clone()));
                            }
                            if ui.small_button("X").clicked() {
                                event_queue.push(crate::events::Event::RemoveEdgeLabel(index, i));
                            }
                            if ui.small_button("+").clicked() {
                                event_queue.push(crate::events::Event::AddEdgeLabel(index, i));
                            }
                        });
                    }
                    if ui.small_button("+ Add Label").clicked() {
                        let edge = &mut graph.graph[index];
                        event_queue.push(crate::events::Event::AddEdgeLabel(index, edge.labels.len()));
                    }
                } else if selected_edges.indices.len() > 1 {
                    ui.label(format!("{} edges selected", selected_edges.indices.len()));
                }

                ui.separator();
            }

            ui.heading("Event Queue");
            let target = event_queue.target();
            let state = event_queue.state();
            let events = event_queue.events();
            
            egui::ScrollArea::vertical()
                .auto_shrink([false; 2])
                .max_height(ui.available_height())
                .show(ui, |ui| {
                    for (i, event) in events.iter().enumerate() {
                        let text = format!("{}: {}", i, event);
                        let label = if i == target {
                            ui.separator();
                            egui::RichText::new(text)
                                .color(egui::Color32::YELLOW)
                                .strong()
                        } else if i < state {
                            egui::RichText::new(text)
                                .color(egui::Color32::DARK_GRAY)
                        } else {
                            egui::RichText::new(text)
                        };
                        ui.label(label);
                    }
                });
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
    editor_settings: Res<EditorSettings>,
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
        if let Ok(screen_pos) = camera.world_to_viewport(
            global_transform, 
            vertex.transform.extend(0.0) * editor_settings.units_per_meter
        ) {
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
        if let Ok(screen_pos) = camera.world_to_viewport(
            global_transform, 
            midpoint.extend(0.0) * editor_settings.units_per_meter
        ) {
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

fn color_picker(ui: &mut egui::Ui, label: &str, color: &mut Color) -> Option<Color> {
    let mut egui_color = bevy_color_to_egui(*color);

    let response = ui.horizontal(|ui| {
        ui.label(label);
        ui.color_edit_button_srgba(&mut egui_color)
    }).inner;

    let popup_id = response.id.with("popup");
    let is_open = egui::Popup::is_id_open(ui.ctx(), popup_id);

    let was_open_id = response.id.with("was_open");
    let orig_color_id = response.id.with("orig_color");

    let was_open: bool = ui.data(|d| d.get_temp(was_open_id).unwrap_or(false));

    if is_open && !was_open {
        ui.data_mut(|d| d.insert_temp(orig_color_id, egui_color));
    }

    let new_color = Color::srgba(
        egui_color.r() as f32 / 255.0,
        egui_color.g() as f32 / 255.0,
        egui_color.b() as f32 / 255.0,
        egui_color.a() as f32 / 255.0,
    );
    *color = new_color;

    let result = if was_open && !is_open {
        let original: Color32 = ui.data(|d| d.get_temp(orig_color_id).unwrap_or(egui_color));
        if original != egui_color {
            Some(new_color)
        } else {
            None
        }
    } else {
        None
    };

    ui.data_mut(|d| d.insert_temp(was_open_id, is_open));

    result
}

fn bevy_color_to_egui(color: Color) -> Color32 {
    let srgba = Srgba::from(color);
    Color32::from_rgb(
        (srgba.red * 255.0) as u8,
        (srgba.green * 255.0) as u8,
        (srgba.blue * 255.0) as u8,
    )
}