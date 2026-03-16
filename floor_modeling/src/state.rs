use std::collections::HashSet;

use serde::{Deserialize, Serialize};

use bevy::prelude::*;
use petgraph::{
    Undirected,
    graph::{EdgeIndex, NodeIndex},
    stable_graph::StableGraph,
};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
pub enum MouseClickStart {
    ClickVertex(NodeIndex),
    ClickEdge(EdgeIndex),
    #[default]
    ClickNothing,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShortcutStart {
    CtrlZ,
    CtrlY,
    U,
    CtrlR,
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

#[derive(Resource, Serialize, Deserialize)]
pub struct EditorState {
    pub mouse_click_start: Option<MouseClickStart>,
    pub shortcut_start: Option<ShortcutStart>,
    pub ui_hovered: bool,
    pub mouse_pos: Option<Vec2>,
}

impl Default for EditorState {
    fn default() -> Self {
        Self {
            mouse_click_start: None,
            shortcut_start: None,
            ui_hovered: false,
            mouse_pos: None,
        }
    }
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct VertexData {
    pub labels: Vec<String>,
    pub transform: Vec2,
}

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct EdgeData {
    pub labels: Vec<String>,
    pub weight: f32,
}

#[derive(Clone, Resource, Default, Serialize, Deserialize)]
pub struct Graph {
    pub graph: StableGraph<VertexData, EdgeData, Undirected>,
}

impl Graph {
    pub fn closest_edge(&self, point: Vec2) -> Option<EdgeIndex> {
        self.graph.edge_indices().min_by(|&a, &b| {
            let dist_a = self.edge_distance(a, point);
            let dist_b = self.edge_distance(b, point);
            dist_a
                .partial_cmp(&dist_b)
                .unwrap_or(std::cmp::Ordering::Equal)
        })
    }

    pub fn closest_edge_dist(&self, point: Vec2) -> Option<(EdgeIndex, f32)> {
        let Some((min, dist_sqr)) = self
            .graph
            .edge_indices()
            .map(|index| {
                let dist = self.edge_distance(index, point);
                (index, dist)
            })
            .min_by(|(_, dist_sqr_a), (_, dist_sqr_b)| {
                dist_sqr_a
                    .partial_cmp(&dist_sqr_b)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
        else {
            return None;
        };

        Some((min, dist_sqr))
    }

    pub fn closest_edge_in_radius(&self, point: Vec2, radius: f32) -> Option<(EdgeIndex, f32)> {
        let Some((min, dist_sqr)) = self
            .graph
            .edge_indices()
            .map(|index| {
                let dist = self.edge_distance(index, point);
                (index, dist)
            })
            .filter(|(_, dist)| *dist < radius)
            .min_by(|(_, dist_sqr_a), (_, dist_sqr_b)| {
                dist_sqr_a
                    .partial_cmp(&dist_sqr_b)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
        else {
            return None;
        };

        Some((min, dist_sqr))
    }

    pub fn edge_distance(&self, edge: EdgeIndex, point: Vec2) -> f32 {
        let (left, right) = self.graph.edge_endpoints(edge).unwrap();
        let a = self.graph[left].transform;
        let b = self.graph[right].transform;
        dist_from_segment(point, a, b)
    }

    pub fn closest_vertex(&self, point: Vec2) -> Option<NodeIndex> {
        self.graph.node_indices().min_by(|&a, &b| {
            let dist_a = point.distance_squared(self.graph[a].transform);
            let dist_b = point.distance_squared(self.graph[b].transform);
            dist_a
                .partial_cmp(&dist_b)
                .unwrap_or(std::cmp::Ordering::Equal)
        })
    }

    pub fn closest_vertex_dist(&self, point: Vec2) -> Option<(NodeIndex, f32)> {
        let Some((min, dist_sqr)) = self
            .graph
            .node_indices()
            .map(|index| {
                let dist_sqr = point.distance_squared(self.graph[index].transform);
                (index, dist_sqr)
            })
            .min_by(|(_, dist_sqr_a), (_, dist_sqr_b)| {
                dist_sqr_a
                    .partial_cmp(&dist_sqr_b)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
        else {
            return None;
        };

        Some((min, dist_sqr.sqrt()))
    }

    pub fn closest_vertex_in_radius(&self, point: Vec2, radius: f32) -> Option<(NodeIndex, f32)> {
        let Some((min, dist_sqr)) = self
            .graph
            .node_indices()
            .map(|index| {
                let dist_sqr = point.distance_squared(self.graph[index].transform);
                (index, dist_sqr)
            })
            .filter(|(_, dist_sqr)| *dist_sqr < radius * radius)
            .min_by(|(_, dist_sqr_a), (_, dist_sqr_b)| {
                dist_sqr_a
                    .partial_cmp(&dist_sqr_b)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
        else {
            return None;
        };

        Some((min, dist_sqr.sqrt()))
    }

    pub fn edges(&self) -> Vec<(&EdgeData, &VertexData, &VertexData)> {
        self.graph
            .edge_indices()
            .map(|index| {
                let (left, right) = self.graph.edge_endpoints(index).unwrap();
                (&self.graph[index], &self.graph[left], &self.graph[right])
            })
            .collect()
    }

    pub fn vertices(&self) -> Vec<&VertexData> {
        self.graph
            .node_indices()
            .map(|index| &self.graph[index])
            .collect()
    }

    pub fn edge_indices(&self) -> Vec<EdgeIndex> {
        self.graph.edge_indices().collect()
    }

    pub fn vertex_indices(&self) -> Vec<NodeIndex> {
        self.graph.node_indices().collect()
    }

    pub fn edge(&self, index: EdgeIndex) -> &EdgeData {
        &self.graph[index]
    }

    pub fn edge_vertex_pair(&self, index: EdgeIndex) -> (&VertexData, &VertexData) {
        let (left, right) = self.graph.edge_endpoints(index).unwrap();
        (&self.graph[left], &self.graph[right])
    }

    pub fn edge_vertex_pair_indices(&self, index: EdgeIndex) -> (NodeIndex, NodeIndex) {
        self.graph.edge_endpoints(index).unwrap()
    }

    pub fn vertex(&self, index: NodeIndex) -> &VertexData {
        &self.graph[index]
    }
}

fn dist_from_segment(point: Vec2, a: Vec2, b: Vec2) -> f32 {
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

#[derive(Resource, Default, Serialize, Deserialize)]
pub struct SelectedVertices {
    pub indices: HashSet<NodeIndex>,
}

#[derive(Resource, Default, Serialize, Deserialize)]
pub struct SelectedEdges {
    pub indices: HashSet<EdgeIndex>,
}

#[derive(Resource, Serialize, Deserialize)]
pub struct GraphSettings {
    pub vertex_color: Color,
    pub vertex_radius: f32,
    pub edge_color: Color,
    pub selected_color: Color,
    pub vertex_selected_radius: f32,
    pub edge_drawing_color: Color,
}

impl Default for GraphSettings {
    fn default() -> Self {
        Self {
            vertex_color: Color::srgb(0.9, 0.9, 0.9),
            vertex_radius: 3.0,
            edge_color: Color::srgb(0.9, 0.9, 0.9),
            selected_color: Color::srgb(0.2, 0.6, 1.0),
            vertex_selected_radius: 4.0,
            edge_drawing_color: Color::srgba(1.0, 1.0, 0.0, 0.7),
        }
    }
}

#[derive(Resource, Serialize, Deserialize)]
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

#[derive(Component, Serialize, Deserialize)]
pub struct BackgroundImage;

#[derive(Resource, Serialize, Deserialize)]
pub struct BackgroundImageSettings {
    pub width_meters: f32,
    pub opacity: f32,
    pub image_path: Option<String>,
    #[serde(skip)]
    pub input_path: Option<String>,
    #[serde(skip)]
    pub image_handle: Option<Handle<Image>>,
    pub aspect_ratio: Option<f32>,
}

impl Default for BackgroundImageSettings {
    fn default() -> Self {
        Self {
            width_meters: 100.0,
            opacity: 0.5,
            image_path: None,
            input_path: None,
            image_handle: None,
            aspect_ratio: None,
        }
    }
}


#[derive(Serialize)]
pub struct SaveDataRef<'a> {
    pub graph: &'a Graph,
    pub selected_vertices: &'a SelectedVertices,
    pub selected_edges: &'a SelectedEdges,
    pub graph_settings: &'a GraphSettings,
    pub grid_settings: &'a GridSettings,
    pub background_settings: &'a BackgroundImageSettings,
}

#[derive(Deserialize)]
pub struct SaveData {
    pub graph: Graph,
    pub selected_vertices: SelectedVertices,
    pub selected_edges: SelectedEdges,
    pub graph_settings: GraphSettings,
    pub grid_settings: GridSettings,
    pub background_settings: BackgroundImageSettings,
}

pub fn save_to_file(
    path: &str,
    graph: &Graph,
    selected_vertices: &SelectedVertices,
    selected_edges: &SelectedEdges,
    graph_settings: &GraphSettings,
    grid_settings: &GridSettings,
    background_settings: &BackgroundImageSettings,
) -> Result<(), Box<dyn std::error::Error>> {
    let data = SaveDataRef {
        graph,
        selected_vertices,
        selected_edges,
        graph_settings,
        grid_settings,
        background_settings,
    };
    let json = serde_json::to_string_pretty(&data)?;
    std::fs::write(path, json)?;
    Ok(())
}

pub fn load_from_file(path: &str) -> Result<SaveData, Box<dyn std::error::Error>> {
    let json = std::fs::read_to_string(path)?;
    let data: SaveData = serde_json::from_str(&json)?;
    Ok(data)
}
