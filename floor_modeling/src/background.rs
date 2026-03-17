use std::error::Error;

use bevy::asset::RenderAssetUsages;
use bevy::image::{CompressedImageFormats, ImageSampler, ImageType};
use bevy::prelude::*;
use bevy::sprite::Anchor;

use crate::events::EventQueue;
use crate::state::{BackgroundImage, BackgroundImageSettings, EditorSettings};

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

pub fn background_sprite_sync(
    bg_settings: Res<BackgroundImageSettings>,
    editor_settings: Res<EditorSettings>,
    mut bg_query: Query<&mut Sprite, With<BackgroundImage>>,
) {
    if !bg_settings.is_changed() {
        return;
    }
    if let Some(aspect_ratio) = bg_settings.aspect_ratio {
        let w = bg_settings.width_meters * editor_settings.units_per_meter;
        let h = w / aspect_ratio;
        for mut sprite in bg_query.iter_mut() {
            sprite.custom_size = Some(Vec2::new(w, h));
            sprite.color = Color::srgba(1.0, 1.0, 1.0, bg_settings.opacity);
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
