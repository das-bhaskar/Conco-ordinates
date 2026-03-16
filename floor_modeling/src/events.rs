use bevy::{color::Color, ecs::resource::Resource, sprite::Anchor};
use petgraph::graph::{EdgeIndex, NodeIndex};
use serde::{Deserialize, Serialize};

use crate::{background::load_image_from_path, state::{BackgroundImage, BackgroundImageSettings, EdgeData, EditorState, Graph, GraphSettings, GridSettings, SelectedEdges, SelectedVertices, ShortcutStart, VertexData}};

use bevy::prelude::*;

pub trait EventTrait {
    fn execute_event(&self);
}

#[derive(Clone, Serialize, Deserialize)]

pub enum Event {
    AddVertex(VertexData),
    AddEdge(NodeIndex, NodeIndex, EdgeData),
    RemoveVertex(NodeIndex),
    RemoveEdge(EdgeIndex),
    RemoveVertexLabel(NodeIndex, usize),
    UpdateVertexLabel(NodeIndex, usize, String),
    AddVertexLabel(NodeIndex, usize),
    RemoveEdgeLabel(EdgeIndex, usize),
    UpdateEdgeLabel(EdgeIndex, usize, String),
    AddEdgeLabel(EdgeIndex, usize),
    ImportBackround(String),
    SetBackgroundWidth(f32),
    SetBackgroundOpacity(f32),
    SetVertexColors(Color),
    SetEdgeColors(Color),
    SetSelectedColors(Color),
    SetDrawingColors(Color),
}

impl Event {
    pub fn execute_event(
        &self, 
        graph: &mut ResMut<Graph>,
        bg_settings: &mut ResMut<BackgroundImageSettings>,
        graph_settings: &mut ResMut<GraphSettings>,
        images: &mut ResMut<Assets<Image>>,
        bg_query: &mut Query<(Entity, &mut Sprite), With<BackgroundImage>>,
        commands: &mut Commands,
    ) {
        match self {
            Event::AddVertex(vertex_data) => {
                graph.graph.add_node(vertex_data.clone());
            },
            Event::AddEdge(left_i, right_i, edge_data) => {
                graph.graph.add_edge(*left_i, *right_i, edge_data.clone());
            },
            Event::RemoveVertex(node_index) => {
                graph.graph.remove_node(*node_index);
            },
            Event::RemoveEdge(edge_index) => {
                graph.graph.remove_edge(*edge_index);
            },
            Event::RemoveVertexLabel(node_index, label_index) => {
                let vertex = &mut graph.graph[*node_index];
                vertex.labels.remove(*label_index);
            },
            Event::UpdateVertexLabel(node_index, label_index, input) => {
                let vertex = &mut graph.graph[*node_index];
                vertex.labels[*label_index] = input.to_string();
            },
            Event::AddVertexLabel(node_index, label_index) => {
                let vertex = &mut graph.graph[*node_index];
                vertex.labels.insert(*label_index, "".to_string());
            },
            Event::RemoveEdgeLabel(edge_index, label_index) => {
                let edge = &mut graph.graph[*edge_index];
                edge.labels.remove(*label_index);
            },
            Event::UpdateEdgeLabel(edge_index, label_index, input) => {
                let edge = &mut graph.graph[*edge_index];
                edge.labels[*label_index] = input.to_string();
            },
            Event::AddEdgeLabel(edge_index, label_index) => {
                let edge = &mut graph.graph[*edge_index];
                edge.labels.insert(*label_index, "".to_string());
            },
            Event::ImportBackround(background_path) => {
                for (entity, _) in bg_query.iter() {
                    commands.entity(entity).despawn();
                }

                match load_image_from_path(&background_path) {
                    Ok((image, aspect_ratio)) => {
                        let handle = images.add(image);
                        bg_settings.image_handle = Some(handle.clone());
                        bg_settings.aspect_ratio = Some(aspect_ratio);
                        bg_settings.image_path = Some(background_path.to_string());

                        let w = bg_settings.width_meters;
                        let h = w / aspect_ratio;

                        commands.spawn((
                            BackgroundImage,
                            Sprite {
                                image: handle,
                                custom_size: Some(Vec2::new(w, h)),
                                color: Color::srgba(1.0, 1.0, 1.0, bg_settings.opacity),
                                ..default()
                            },
                            Anchor::BOTTOM_LEFT,
                            Transform::from_xyz(0.0, 0.0, -0.5),
                        ));
                    }
                    Err(e) => {
                        eprintln!("Failed to load background image: {}", e);
                    }
                }
            },
            Event::SetBackgroundWidth(w) => {
                bg_settings.width_meters = *w;
                if let Some(aspect_ratio) = bg_settings.aspect_ratio {
                    let h = w / aspect_ratio;
                    for (_, mut sprite) in bg_query.iter_mut() {
                        sprite.custom_size = Some(Vec2::new(*w, h));
                    }
                }
            },
            Event::SetBackgroundOpacity(op) => {
                for (_, mut sprite) in bg_query.iter_mut() {
                    sprite.color = Color::srgba(1.0, 1.0, 1.0, bg_settings.opacity);
                }
            },
            Event::SetVertexColors(color) => {
                graph_settings.vertex_color = *color;
            },
            Event::SetEdgeColors(color) => {
                graph_settings.edge_color = *color;
            },
            Event::SetSelectedColors(color) => {
                graph_settings.selected_color = *color;
            },
            Event::SetDrawingColors(color) =>{
                graph_settings.edge_drawing_color = *color;
            },
        }
    }
}

#[derive(Clone, Resource, Default, Serialize, Deserialize)]
pub struct EventQueue {
    events: Vec<Event>,
    state: usize,
    target: usize,
}

impl EventQueue {
    pub fn new() -> EventQueue {
        EventQueue {
            events: vec![],
            state: 0,
            target: 0,
        }
    }

    pub fn events(&self) -> &[Event] {
        &self.events
    }
    
    pub fn state(&self) -> usize {
        self.state
    }
    
    pub fn target(&self) -> usize {
        self.target
    }

    pub fn remaining_to_target(&self) -> Vec<Event> {
        self.events[self.state..self.target].to_vec()
    }

    pub fn set_state_to_target(&mut self) {
        self.state = self.target;
        self.assertions();
    }

    pub fn undo(&mut self) {
        self.state = 0;
        if self.target != 0 {
            self.target -= 1;
        }
        self.assertions();
    }

    pub fn redo(&mut self) {
        self.state = 0;
        self.target += 1;
        self.target = self.target.min(self.events.len());
        self.assertions();
    }

    pub fn push(&mut self, event: Event) {
        self.events.truncate(self.target);
        self.events.push(event);
        self.target = 0.max(self.events.len());
        self.state = self.state.min(self.target);
        self.assertions();
    }

    fn assertions(&self) {
        assert!(self.state <= self.events.len());
        assert!(self.target <= self.events.len());
        assert!(self.state <= self.target);
    }
}

impl std::fmt::Display for Event {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Event::AddVertex(vertex_data) => write!(f, "Add Vertex: {:?}", vertex_data),
            Event::AddEdge(a, b, edge_data) => write!(f, "Add Edge ({:?}-{:?}, {:?})", a, b, edge_data),
            Event::RemoveVertex(i) => write!(f, "Remove Vertex {:?}", i),
            Event::RemoveEdge(i) => write!(f, "Remove Edge {:?}", i),
            Event::RemoveVertexLabel(i, l) => write!(f, "Remove Vertex Label ({:?}, {})", i, l),
            Event::UpdateVertexLabel(i, l, text) => write!(f, "Update Vertex Label ({:?}, {}, {})", i, l, text),
            Event::AddVertexLabel(i, l) => write!(f, "Add Vertex Label ({:?}, {})", i, l),
            Event::RemoveEdgeLabel(i, l) => write!(f, "Remove Edge Label ({:?}, {})", i, l),
            Event::UpdateEdgeLabel(i, l, text) => write!(f, "Update Edge Label ({:?}, {}, {})", i, l, text),
            Event::AddEdgeLabel(i, l) => write!(f, "Add Edge Label ({:?}, {})", i, l),
            Event::ImportBackround(path) => write!(f, "Import Background({})", {path}),
            Event::SetBackgroundWidth(w) => write!(f, "Set BG Width {:.1}", w),
            Event::SetBackgroundOpacity(o) => write!(f, "Set BG Opacity {:.2}", o),
            Event::SetVertexColors(color) => write!(f, "Set Vertex Colors: {:?}", color),
            Event::SetEdgeColors(color) => write!(f, "Set Edge Colors: {:?}", color),
            Event::SetSelectedColors(color) => write!(f, "Set Selected Colors: {:?}", color),
            Event::SetDrawingColors(color) => write!(f, "Set Drawing Colors: {:?}", color),
        }
    }
}

pub fn event_system(
    mut editor: ResMut<EditorState>,
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut graph_settings: ResMut<GraphSettings>,
    mut graph: ResMut<Graph>,
    mut images: ResMut<Assets<Image>>,
    mut commands: Commands,
    mut bg_query: Query<(Entity, &mut Sprite), With<BackgroundImage>>,
    mut selected_vertices: ResMut<SelectedVertices>,
    mut selected_edges: ResMut<SelectedEdges>,
    mut contexts: bevy_egui::EguiContexts,
    mut event_queue: ResMut<EventQueue>,
    keyboard: Res<ButtonInput<KeyCode>>,
) {
    let typing = contexts.ctx_mut().map(|ctx| ctx.wants_keyboard_input()).unwrap_or(false);

    let mut found_shortcut = false;

    if !typing {
        if keyboard.pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyZ) {
            editor.shortcut_start = Some(ShortcutStart::CtrlZ);
            found_shortcut = true;
        }

        if keyboard.pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyY) {
            editor.shortcut_start = Some(ShortcutStart::CtrlY);
            found_shortcut = true;
        }

        if keyboard.just_pressed(KeyCode::KeyU) {
            editor.shortcut_start = Some(ShortcutStart::CtrlZ);
            found_shortcut = true;
        }

        if keyboard.pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyR) {
            editor.shortcut_start = Some(ShortcutStart::CtrlR);
            found_shortcut = true;
        }
    }

    match editor.shortcut_start {
        Some(shortcut_start) => {
            match shortcut_start {
                ShortcutStart::CtrlZ => {
                    if !found_shortcut {
                        event_queue.undo();
                        editor.shortcut_start = None;
                    }
                },
                ShortcutStart::CtrlY => {
                    if !found_shortcut {
                        event_queue.redo();
                        editor.shortcut_start = None;
                    }
                },
                ShortcutStart::U => {
                    if !found_shortcut {
                        event_queue.undo();
                        editor.shortcut_start = None;
                    }
                },
                ShortcutStart::CtrlR => {
                    if !found_shortcut {
                        event_queue.redo();
                        editor.shortcut_start = None;
                    }
                },
            }
        },
        None => {
            if !found_shortcut {
                editor.shortcut_start = None;
            }
        },
    }

    let events_todo = event_queue.remaining_to_target();

    if event_queue.state() == 0 {
        graph.graph.clear();
        graph_settings.vertex_color = GraphSettings::default().vertex_color;
        graph_settings.edge_color = GraphSettings::default().edge_color;
        graph_settings.selected_color = GraphSettings::default().selected_color;
        graph_settings.edge_drawing_color = GraphSettings::default().edge_drawing_color;
        *bg_settings = BackgroundImageSettings::default();
        selected_vertices.indices.clear();
        selected_edges.indices.clear();
        for (entity, _) in bg_query.iter() {
            commands.entity(entity).despawn();
        }
    }

    for event in events_todo {
        event.execute_event(&mut graph, &mut bg_settings, &mut graph_settings, &mut images, &mut bg_query, &mut commands);
    }

    event_queue.set_state_to_target();
}