use std::error::Error;

use bevy::asset::RenderAssetUsages;
use bevy::image::{CompressedImageFormats, ImageSampler, ImageType};
use bevy::prelude::*;
use bevy::sprite::Anchor;

use crate::state::{BackgroundImage, BackgroundImageSettings};

pub fn background_image_system(
    mut commands: Commands,
    mut bg_settings: ResMut<BackgroundImageSettings>,
    mut images: ResMut<Assets<Image>>,
    mut bg_query: Query<(Entity, &mut Sprite, &mut Visibility), With<BackgroundImage>>,
) {
    match bg_settings.input_path.take() {
        Some(path) => {
            for (entity, _, _) in bg_query.iter() {
                commands.entity(entity).despawn();
            }

            match load_image_from_path(&path) {
                Ok((image, aspect_ratio)) => {
                    let handle = images.add(image);
                    bg_settings.image_handle = Some(handle.clone());
                    bg_settings.aspect_ratio = Some(aspect_ratio);
                    bg_settings.image_path = Some(path);

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
        }
        None => {
            for (_, mut sprite, mut visibility) in bg_query.iter_mut() {
                if let Some(aspect_ratio) = bg_settings.aspect_ratio {
                    let w = bg_settings.width_meters;
                    let h = w / aspect_ratio;
                    sprite.custom_size = Some(Vec2::new(w, h));
                }
                sprite.color = Color::srgba(1.0, 1.0, 1.0, bg_settings.opacity);
                *visibility = if bg_settings.visible {
                    Visibility::Visible
                } else {
                    Visibility::Hidden
                };
            }
        }
    }
}

fn load_image_from_path(path: &str) -> Result<(Image, f32), Box<dyn Error>> {
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
