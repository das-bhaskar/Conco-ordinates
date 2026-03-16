use bevy::{color::Color, ecs::resource::Resource};
use petgraph::graph::{EdgeIndex, NodeIndex};
use serde::{Deserialize, Serialize};

use crate::state::{BackgroundImageSettings, EdgeData, EditorState, Graph, GraphSettings, GridSettings, SelectedEdges, SelectedVertices, ShortcutStart, VertexData};

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
    RemoveSelected(Vec<NodeIndex>, Vec<EdgeIndex>),
    RemoveVertexLabel(NodeIndex, usize),
    UpdateVertexLabel(NodeIndex, usize, String),
    AddVertexLabel(NodeIndex, String),
    RemoveEdgeLabel(EdgeIndex, usize),
    UpdateEdgeLabel(EdgeIndex, usize, String),
    AddEdgeLabel(EdgeIndex, String),
    ImportBackround(String),
    SetBackgroundWidth(f32),
    SetBackgroundOpacity(f32),
    SetBackgroundVisible(f32),
    SetVertexColors(Color),
    SetEdgeColors(Color),
    SetSelectedColors(Color),
    SetDrawingColors(Color),
}

impl Event {
    pub fn execute_event(
        &self, 
        graph: &mut ResMut<Graph>,
        selected_vertices: &mut ResMut<SelectedVertices>,
        selected_edges: &mut ResMut<SelectedEdges>,
        grid_settings: &mut ResMut<GridSettings>,
        bg_settings: &mut ResMut<BackgroundImageSettings>,
        graph_settings: &mut ResMut<GraphSettings>,
    ) {
        match self {
            Event::AddVertex(vertex_data) => {
                graph.graph.add_node(vertex_data.clone());
            },
            Event::AddEdge(left_i, right_i, edge_data) => {
                graph.graph.add_edge(*left_i, *right_i, edge_data.clone());
            },
            Event::RemoveVertex(node_index) => todo!(),
            Event::RemoveEdge(edge_index) => todo!(),
            Event::RemoveSelected(items, items1) => todo!(),
            Event::RemoveVertexLabel(node_index, _) => todo!(),
            Event::UpdateVertexLabel(node_index, _, _) => todo!(),
            Event::AddVertexLabel(node_index, _) => todo!(),
            Event::RemoveEdgeLabel(edge_index, _) => todo!(),
            Event::UpdateEdgeLabel(edge_index, _, _) => todo!(),
            Event::AddEdgeLabel(edge_index, _) => todo!(),
            Event::ImportBackround(_) => todo!(),
            Event::SetBackgroundWidth(_) => todo!(),
            Event::SetBackgroundOpacity(_) => todo!(),
            Event::SetBackgroundVisible(_) => todo!(),
            Event::SetVertexColors(color) => todo!(),
            Event::SetEdgeColors(color) => todo!(),
            Event::SetSelectedColors(color) => todo!(),
            Event::SetDrawingColors(color) => todo!(),
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

    pub fn remaining_to_target(&self) -> Vec<Event> {
        self.events[self.state..self.target].to_vec()
    }

    pub fn set_state_to_target(&mut self) {
        self.state = self.target;
        self.assertions();
    }

    pub fn undo(&mut self) {
        self.state = 0;
        self.target -= 1;
        self.assertions();
    }

    pub fn push(&mut self, event: Event) {
        self.events.truncate(self.target);
        self.events.push(event);
        self.target = 0.max(self.events.len() - 1);
        self.state = self.state.min(self.target);
        self.assertions();
    }

    fn assertions(&self) {
        assert!(self.state <= self.events.len());
        assert!(self.target <= self.events.len());
        assert!(self.state <= self.target);
    }
}

pub fn event_system(
    mut editor: ResMut<EditorState>,
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut graph_settings: ResMut<GraphSettings>,
    mut graph: ResMut<Graph>,
    mut selected_vertices: ResMut<SelectedVertices>,
    mut selected_edges: ResMut<SelectedEdges>,
    mut grid_settings: ResMut<GridSettings>,
    keyboard: Res<ButtonInput<KeyCode>>,
) {
    let mut found_shortcut = false;
    if keyboard.just_pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyZ) {
        editor.shortcut_start = Some(ShortcutStart::CtrlZ);
        found_shortcut = true;
    }

    if keyboard.just_pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyY) {
        editor.shortcut_start = Some(ShortcutStart::CtrlY);
        found_shortcut = true;
    }

    if keyboard.just_pressed(KeyCode::KeyU) {
        editor.shortcut_start = Some(ShortcutStart::CtrlZ);
        found_shortcut = true;
    }

    if keyboard.just_pressed(KeyCode::ControlLeft) && keyboard.just_pressed(KeyCode::KeyR) {
        editor.shortcut_start = Some(ShortcutStart::CtrlZ);
        found_shortcut = true;
    }

    match editor.shortcut_start {
        Some(shortcut_start) => {
            match shortcut_start {
                ShortcutStart::CtrlZ => todo!(),
                ShortcutStart::CtrlY => todo!(),
                ShortcutStart::U => todo!(),
                ShortcutStart::CtrlR => todo!(),
            }
        },
        None => {
            if !found_shortcut {
                editor.shortcut_start = None;
            }
        },
    }
}