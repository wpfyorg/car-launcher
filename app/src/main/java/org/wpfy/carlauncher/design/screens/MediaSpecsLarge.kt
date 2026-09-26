package org.wpfy.carlauncher.design.screens

import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeL.MediaScreen.Pane as LPane
import org.wpfy.carlauncher.design.kit.PenScreens.LandscapeXL.MediaScreen.Pane as XLPane
import org.wpfy.carlauncher.design.kit.PenScreens.UltrawideL.MediaScreen.Pane as ULPane

// Measures of the Pen media panes of the Landscape L, Ultrawide L and Landscape XL media screens, read from the
// generated PenScreens tokens (one spec per Pen frame group).

internal val LandscapeLMediaSpec = MediaPaneSpec(
    radius = LPane.Radius,
    gradient = LPane.Gradient,
    rotation = LPane.GradientRotation,
    source = MediaSource(MediaFrame(LPane.Source.X, LPane.Source.Y, LPane.Source.W, LPane.Source.H), MediaFrame(LPane.Source.Icon.X, LPane.Source.Icon.Y, LPane.Source.Icon.W, LPane.Source.Icon.H), LPane.Source.Icon.Fill, MediaFrame(LPane.Source.Label.X, LPane.Source.Label.Y, LPane.Source.Label.W, LPane.Source.Label.H), LPane.Source.Label.Style, LPane.Source.Label.Fill),
    artwork = MediaFrame(LPane.Artwork.X, LPane.Artwork.Y, LPane.Artwork.W, LPane.Artwork.H), artworkRadius = LPane.Artwork.Radius, artworkGradient = LPane.Artwork.Gradient, artworkRotation = LPane.Artwork.GradientRotation,
    artworkIcon = MediaFrame(LPane.Artwork.Icon.X, LPane.Artwork.Icon.Y, LPane.Artwork.Icon.W, LPane.Artwork.Icon.H), artworkIconFill = LPane.Artwork.Icon.Fill,
    text = MediaFrame(LPane.Text.X, LPane.Text.Y, LPane.Text.W, LPane.Text.H), title = MediaFrame(LPane.Text.Title.X, LPane.Text.Title.Y, LPane.Text.Title.W, LPane.Text.Title.H), titleStyle = LPane.Text.Title.Style, titleFill = LPane.Text.Title.Fill,
    artist = MediaFrame(LPane.Text.Artist.X, LPane.Text.Artist.Y, LPane.Text.Artist.W, LPane.Text.Artist.H), artistStyle = LPane.Text.Artist.Style, artistFill = LPane.Text.Artist.Fill,
    progress = MediaFrame(LPane.Progress.X, LPane.Progress.Y, LPane.Progress.W, LPane.Progress.H), track = MediaFrame(LPane.Progress.Track.X, LPane.Progress.Track.Y, LPane.Progress.Track.W, LPane.Progress.Track.H), trackRadius = LPane.Progress.Track.Radius, trackFill = LPane.Progress.Track.Fill,
    doneRadius = LPane.Progress.Track.Done.Radius, doneFill = LPane.Progress.Track.Done.Fill,
    times = MediaFrame(LPane.Progress.Times.X, LPane.Progress.Times.Y, LPane.Progress.Times.W, LPane.Progress.Times.H), elapsed = MediaFrame(LPane.Progress.Times.Elapsed.X, LPane.Progress.Times.Elapsed.Y, LPane.Progress.Times.Elapsed.W, LPane.Progress.Times.Elapsed.H), total = MediaFrame(LPane.Progress.Times.Total.X, LPane.Progress.Times.Total.Y, LPane.Progress.Times.Total.W, LPane.Progress.Times.Total.H),
    timesStyle = LPane.Progress.Times.Elapsed.Style, timesFill = LPane.Progress.Times.Elapsed.Fill,
    transport = MediaFrame(LPane.Transport.X, LPane.Transport.Y, LPane.Transport.W, LPane.Transport.H), transportGap = null,
    previous = MediaDisc(MediaFrame(LPane.Transport.SkipPrevious.X, LPane.Transport.SkipPrevious.Y, LPane.Transport.SkipPrevious.W, LPane.Transport.SkipPrevious.H), LPane.Transport.SkipPrevious.Fill, MediaFrame(LPane.Transport.SkipPrevious.Icon.X, LPane.Transport.SkipPrevious.Icon.Y, LPane.Transport.SkipPrevious.Icon.W, LPane.Transport.SkipPrevious.Icon.H), LPane.Transport.SkipPrevious.Icon.Fill),
    pause = MediaDisc(MediaFrame(LPane.Transport.Pause.X, LPane.Transport.Pause.Y, LPane.Transport.Pause.W, LPane.Transport.Pause.H), LPane.Transport.Pause.Fill, MediaFrame(LPane.Transport.Pause.Icon.X, LPane.Transport.Pause.Icon.Y, LPane.Transport.Pause.Icon.W, LPane.Transport.Pause.Icon.H), LPane.Transport.Pause.Icon.Fill),
    next = MediaDisc(MediaFrame(LPane.Transport.SkipNext.X, LPane.Transport.SkipNext.Y, LPane.Transport.SkipNext.W, LPane.Transport.SkipNext.H), LPane.Transport.SkipNext.Fill, MediaFrame(LPane.Transport.SkipNext.Icon.X, LPane.Transport.SkipNext.Icon.Y, LPane.Transport.SkipNext.Icon.W, LPane.Transport.SkipNext.Icon.H), LPane.Transport.SkipNext.Icon.Fill),
)

internal val UltrawideLMediaSpec = MediaPaneSpec(
    radius = ULPane.Radius,
    gradient = ULPane.Gradient,
    rotation = ULPane.GradientRotation,
    source = MediaSource(MediaFrame(ULPane.Source.X, ULPane.Source.Y, ULPane.Source.W, ULPane.Source.H), MediaFrame(ULPane.Source.Icon.X, ULPane.Source.Icon.Y, ULPane.Source.Icon.W, ULPane.Source.Icon.H), ULPane.Source.Icon.Fill, MediaFrame(ULPane.Source.Label.X, ULPane.Source.Label.Y, ULPane.Source.Label.W, ULPane.Source.Label.H), ULPane.Source.Label.Style, ULPane.Source.Label.Fill),
    artwork = MediaFrame(ULPane.Artwork.X, ULPane.Artwork.Y, ULPane.Artwork.W, ULPane.Artwork.H), artworkRadius = ULPane.Artwork.Radius, artworkGradient = ULPane.Artwork.Gradient, artworkRotation = ULPane.Artwork.GradientRotation,
    artworkIcon = MediaFrame(ULPane.Artwork.Icon.X, ULPane.Artwork.Icon.Y, ULPane.Artwork.Icon.W, ULPane.Artwork.Icon.H), artworkIconFill = ULPane.Artwork.Icon.Fill,
    text = MediaFrame(ULPane.Text.X, ULPane.Text.Y, ULPane.Text.W, ULPane.Text.H), title = MediaFrame(ULPane.Text.Title.X, ULPane.Text.Title.Y, ULPane.Text.Title.W, ULPane.Text.Title.H), titleStyle = ULPane.Text.Title.Style, titleFill = ULPane.Text.Title.Fill,
    artist = MediaFrame(ULPane.Text.Artist.X, ULPane.Text.Artist.Y, ULPane.Text.Artist.W, ULPane.Text.Artist.H), artistStyle = ULPane.Text.Artist.Style, artistFill = ULPane.Text.Artist.Fill,
    progress = MediaFrame(ULPane.Progress.X, ULPane.Progress.Y, ULPane.Progress.W, ULPane.Progress.H), track = MediaFrame(ULPane.Progress.Track.X, ULPane.Progress.Track.Y, ULPane.Progress.Track.W, ULPane.Progress.Track.H), trackRadius = ULPane.Progress.Track.Radius, trackFill = ULPane.Progress.Track.Fill,
    doneRadius = ULPane.Progress.Track.Done.Radius, doneFill = ULPane.Progress.Track.Done.Fill,
    times = MediaFrame(ULPane.Progress.Times.X, ULPane.Progress.Times.Y, ULPane.Progress.Times.W, ULPane.Progress.Times.H), elapsed = MediaFrame(ULPane.Progress.Times.Elapsed.X, ULPane.Progress.Times.Elapsed.Y, ULPane.Progress.Times.Elapsed.W, ULPane.Progress.Times.Elapsed.H), total = MediaFrame(ULPane.Progress.Times.Total.X, ULPane.Progress.Times.Total.Y, ULPane.Progress.Times.Total.W, ULPane.Progress.Times.Total.H),
    timesStyle = ULPane.Progress.Times.Elapsed.Style, timesFill = ULPane.Progress.Times.Elapsed.Fill,
    transport = MediaFrame(ULPane.Transport.X, ULPane.Transport.Y, ULPane.Transport.W, ULPane.Transport.H), transportGap = null,
    previous = MediaDisc(MediaFrame(ULPane.Transport.SkipPrevious.X, ULPane.Transport.SkipPrevious.Y, ULPane.Transport.SkipPrevious.W, ULPane.Transport.SkipPrevious.H), ULPane.Transport.SkipPrevious.Fill, MediaFrame(ULPane.Transport.SkipPrevious.Icon.X, ULPane.Transport.SkipPrevious.Icon.Y, ULPane.Transport.SkipPrevious.Icon.W, ULPane.Transport.SkipPrevious.Icon.H), ULPane.Transport.SkipPrevious.Icon.Fill),
    pause = MediaDisc(MediaFrame(ULPane.Transport.Pause.X, ULPane.Transport.Pause.Y, ULPane.Transport.Pause.W, ULPane.Transport.Pause.H), ULPane.Transport.Pause.Fill, MediaFrame(ULPane.Transport.Pause.Icon.X, ULPane.Transport.Pause.Icon.Y, ULPane.Transport.Pause.Icon.W, ULPane.Transport.Pause.Icon.H), ULPane.Transport.Pause.Icon.Fill),
    next = MediaDisc(MediaFrame(ULPane.Transport.SkipNext.X, ULPane.Transport.SkipNext.Y, ULPane.Transport.SkipNext.W, ULPane.Transport.SkipNext.H), ULPane.Transport.SkipNext.Fill, MediaFrame(ULPane.Transport.SkipNext.Icon.X, ULPane.Transport.SkipNext.Icon.Y, ULPane.Transport.SkipNext.Icon.W, ULPane.Transport.SkipNext.Icon.H), ULPane.Transport.SkipNext.Icon.Fill),
)

internal val LandscapeXLMediaSpec = MediaPaneSpec(
    radius = XLPane.Radius,
    gradient = XLPane.Gradient,
    rotation = XLPane.GradientRotation,
    source = MediaSource(MediaFrame(XLPane.Source.X, XLPane.Source.Y, XLPane.Source.W, XLPane.Source.H), MediaFrame(XLPane.Source.Icon.X, XLPane.Source.Icon.Y, XLPane.Source.Icon.W, XLPane.Source.Icon.H), XLPane.Source.Icon.Fill, MediaFrame(XLPane.Source.Label.X, XLPane.Source.Label.Y, XLPane.Source.Label.W, XLPane.Source.Label.H), XLPane.Source.Label.Style, XLPane.Source.Label.Fill),
    artwork = MediaFrame(XLPane.Artwork.X, XLPane.Artwork.Y, XLPane.Artwork.W, XLPane.Artwork.H), artworkRadius = XLPane.Artwork.Radius, artworkGradient = XLPane.Artwork.Gradient, artworkRotation = XLPane.Artwork.GradientRotation,
    artworkIcon = MediaFrame(XLPane.Artwork.Icon.X, XLPane.Artwork.Icon.Y, XLPane.Artwork.Icon.W, XLPane.Artwork.Icon.H), artworkIconFill = XLPane.Artwork.Icon.Fill,
    text = MediaFrame(XLPane.Text.X, XLPane.Text.Y, XLPane.Text.W, XLPane.Text.H), title = MediaFrame(XLPane.Text.Title.X, XLPane.Text.Title.Y, XLPane.Text.Title.W, XLPane.Text.Title.H), titleStyle = XLPane.Text.Title.Style, titleFill = XLPane.Text.Title.Fill,
    artist = MediaFrame(XLPane.Text.Artist.X, XLPane.Text.Artist.Y, XLPane.Text.Artist.W, XLPane.Text.Artist.H), artistStyle = XLPane.Text.Artist.Style, artistFill = XLPane.Text.Artist.Fill,
    progress = MediaFrame(XLPane.Progress.X, XLPane.Progress.Y, XLPane.Progress.W, XLPane.Progress.H), track = MediaFrame(XLPane.Progress.Track.X, XLPane.Progress.Track.Y, XLPane.Progress.Track.W, XLPane.Progress.Track.H), trackRadius = XLPane.Progress.Track.Radius, trackFill = XLPane.Progress.Track.Fill,
    doneRadius = XLPane.Progress.Track.Done.Radius, doneFill = XLPane.Progress.Track.Done.Fill,
    times = MediaFrame(XLPane.Progress.Times.X, XLPane.Progress.Times.Y, XLPane.Progress.Times.W, XLPane.Progress.Times.H), elapsed = MediaFrame(XLPane.Progress.Times.Elapsed.X, XLPane.Progress.Times.Elapsed.Y, XLPane.Progress.Times.Elapsed.W, XLPane.Progress.Times.Elapsed.H), total = MediaFrame(XLPane.Progress.Times.Total.X, XLPane.Progress.Times.Total.Y, XLPane.Progress.Times.Total.W, XLPane.Progress.Times.Total.H),
    timesStyle = XLPane.Progress.Times.Elapsed.Style, timesFill = XLPane.Progress.Times.Elapsed.Fill,
    transport = MediaFrame(XLPane.Transport.X, XLPane.Transport.Y, XLPane.Transport.W, XLPane.Transport.H), transportGap = null,
    previous = MediaDisc(MediaFrame(XLPane.Transport.SkipPrevious.X, XLPane.Transport.SkipPrevious.Y, XLPane.Transport.SkipPrevious.W, XLPane.Transport.SkipPrevious.H), XLPane.Transport.SkipPrevious.Fill, MediaFrame(XLPane.Transport.SkipPrevious.Icon.X, XLPane.Transport.SkipPrevious.Icon.Y, XLPane.Transport.SkipPrevious.Icon.W, XLPane.Transport.SkipPrevious.Icon.H), XLPane.Transport.SkipPrevious.Icon.Fill),
    pause = MediaDisc(MediaFrame(XLPane.Transport.Pause.X, XLPane.Transport.Pause.Y, XLPane.Transport.Pause.W, XLPane.Transport.Pause.H), XLPane.Transport.Pause.Fill, MediaFrame(XLPane.Transport.Pause.Icon.X, XLPane.Transport.Pause.Icon.Y, XLPane.Transport.Pause.Icon.W, XLPane.Transport.Pause.Icon.H), XLPane.Transport.Pause.Icon.Fill),
    next = MediaDisc(MediaFrame(XLPane.Transport.SkipNext.X, XLPane.Transport.SkipNext.Y, XLPane.Transport.SkipNext.W, XLPane.Transport.SkipNext.H), XLPane.Transport.SkipNext.Fill, MediaFrame(XLPane.Transport.SkipNext.Icon.X, XLPane.Transport.SkipNext.Icon.Y, XLPane.Transport.SkipNext.Icon.W, XLPane.Transport.SkipNext.Icon.H), XLPane.Transport.SkipNext.Icon.Fill),
)
