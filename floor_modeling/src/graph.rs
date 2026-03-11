use std::collections::HashSet;

use bevy::{
    prelude::*,
};
use bevy_egui::EguiContext;

use crate::state::{EdgeData, EditorSettings, EditorState, Graph, GraphSettings, MouseClickStart, SelectedEdges, SelectedVertices, VertexData};

pub fn graph_operations(
    mut editor: ResMut<EditorState>,
    editor_settings: Res<EditorSettings>,
    mut graph: ResMut<Graph>,
    mut selected_vertices: ResMut<SelectedVertices>,
    mut selected_edges: ResMut<SelectedEdges>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    camera_query: Single<(&Camera, &GlobalTransform), (With<Camera2d>, Without<EguiContext>)>,
    window: Single<&Window>,
) {
    if keyboard.just_pressed(KeyCode::Delete) {
        for edge in selected_edges.indices.clone() {
            graph.graph.remove_edge(edge);
        }
        for vertex in selected_vertices.indices.clone() {
            graph.graph.remove_node(vertex);
        }
        selected_edges.indices = HashSet::new();
        selected_vertices.indices = HashSet::new();
    }
    
    if editor.ui_hovered {
        return;
    }

    editor.mouse_pos = None;

    let (camera, global_transform) = camera_query.into_inner();

    let cursor_pos = match window.cursor_position() {
        Some(cursor_pos) => cursor_pos,
        None => {
            return;
        }
    };

    let cursor_world_pos = match camera.viewport_to_world_2d(global_transform, cursor_pos) {
        Ok(cursor_world_pos) => cursor_world_pos,
        Err(_) => {
            return;
        }
    };

    editor.mouse_pos = Some(cursor_world_pos);

    let closest_edge = graph.closest_edge_in_radius(cursor_world_pos, editor_settings.select_threshold);

    let closest_vertex = graph.closest_vertex_in_radius(cursor_world_pos, editor_settings.select_threshold);

    match editor.mouse_click_start {
        Some(being_clicked) => {
            match being_clicked {
                crate::state::MouseClickStart::ClickVertex(prev_index) => {
                    // Already clicking a vertex before this frame
                    if mouse_button.just_released(MouseButton::Left) {
                        match (closest_edge, closest_vertex) {
                            (_, Some((vertex, _))) => {
                                if vertex != prev_index {
                                    // Dragged to new vertex
                                    let start_transform = graph.vertex(prev_index).transform;
                                    let end_transform = graph.vertex(vertex).transform;
                                    let label = "".to_string();
                                    graph.graph.add_edge(prev_index, vertex, EdgeData {
                                        label: label,
                                        weight: end_transform.distance(start_transform).abs(),
                                    });
                                }
                            },
                            _ => {},
                        }
                        editor.mouse_click_start = None;
                    }
                },
                crate::state::MouseClickStart::ClickEdge(_) => {
                    // Already clicking an edge before this frame
                    if mouse_button.just_released(MouseButton::Left) {
                        editor.mouse_click_start = None;
                    }
                },
                crate::state::MouseClickStart::ClickNothing => {
                    // Already clicking nothing before this frame
                    if mouse_button.just_released(MouseButton::Left) {
                        match (closest_edge, closest_vertex) {
                            (None, None) => {
                                // Draw vertex
                                let label = "".to_string();
                                graph.graph.add_node(VertexData {
                                    label: label,
                                    transform: cursor_world_pos,
                                });
                            },
                            _ => {
                                // Released mouse but can't draw
                            },
                        }
                        editor.mouse_click_start = None;
                    }
                },
            }
        },
        None => {
            // Not clicking before this frame
            if mouse_button.just_pressed(MouseButton::Left) {
                let shift_held = keyboard.pressed(KeyCode::ShiftLeft);

                if !shift_held {
                    selected_edges.indices = HashSet::new();
                    selected_vertices.indices = HashSet::new();
                }

                // Begun clicking
                if let Some((v_index, v_dist)) = closest_vertex && v_dist < editor_settings.select_threshold {
                    // Clicking vertex
                    editor.mouse_click_start = Some(MouseClickStart::ClickVertex(v_index));
                    selected_vertices.indices.insert(v_index);
                }
                else if let Some((e_index, e_dist)) = closest_edge && e_dist < editor_settings.select_threshold {
                    // Clicking vertex
                    editor.mouse_click_start = Some(MouseClickStart::ClickEdge(e_index));
                    selected_edges.indices.insert(e_index);
                }
                else {
                    editor.mouse_click_start = Some(MouseClickStart::ClickNothing);
                }
            }
        },
    }
}

pub fn render_graph(
    mut gizmos: Gizmos,
    graph: Res<Graph>,
    selected_vertices: Res<SelectedVertices>,
    selected_edges: Res<SelectedEdges>,
    settings: Res<GraphSettings>,
    editor: Res<EditorState>,
) {
    for index in graph.vertex_indices() {
        let (color, radius) = if selected_vertices.indices.contains(&index) {
            (settings.selected_color, settings.vertex_selected_radius)
        } else {
            (settings.vertex_color, settings.vertex_radius)
        };
        gizmos.circle_2d(graph.vertex(index).transform, radius, color);
    }

    for index in graph.edge_indices() {
        let color = if selected_edges.indices.contains(&index) {
            settings.selected_color
        } else {
            settings.edge_color
        };
        let (left, right) = graph.edge_vertex_pair(index);
        gizmos.line_2d(left.transform, right.transform, color);
    }

    if let Some(click_type) = editor.mouse_click_start && let MouseClickStart::ClickVertex(clicked_v) = click_type {
        if let Some(mouse_pos) = editor.mouse_pos {
            gizmos.line_2d(graph.vertex(clicked_v).transform, mouse_pos, settings.edge_drawing_color);
        }
    }
}
