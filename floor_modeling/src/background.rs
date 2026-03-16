use std::error::Error;

use bevy::asset::RenderAssetUsages;
use bevy::image::{CompressedImageFormats, ImageSampler, ImageType};
use bevy::prelude::*;
use bevy::sprite::Anchor;

use crate::events::EventQueue;
use crate::state::{BackgroundImage, BackgroundImageSettings};

pub fn background_image_system(
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut event_queue: ResMut<EventQueue>,
) {
    match bg_settings.input_path.take() {
        Some(path) => {
            event_queue.push(crate::events::Event::ImportBackround(path))
        }
        None => {
        }
    }
}

pub fn load_image_from_path(path: &str) -> Result<(Image, f32), Box<dyn Error>> {
    let bytes = std::fs::read(path)?;
    let ext = std::path::Path::new(path)
        .extension()
        .and_then(|e| e.to_str())
        .unwrap_or("png");

    let image = Image::from_buffer(
        &bytes,
        ImageType::Extension(ext),
        CompressedImageFormats::NONE,
        true,
        ImageSampler::default(),
        RenderAssetUsages::default(),
    )?;

    let w = image.width() as f32;
    let h = image.height() as f32;
    let aspect_ratio = w / h;

    Ok((image, aspect_ratio))
}
