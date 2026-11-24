package xyz.ksharma.krail.core.testing.device

/**
 * The devices every screen is composed on. The table, the reasoning for each shape and every row's source are in
 * `docs/testing/DEVICE_PROFILES.md`; keep the two in step (a test fails when a name is missing from that page).
 *
 * A number is `measured` only where a probe printed it, and its source says which probe. Everything else is `assumed` and
 * says why. Never promote an assumed number without a probe.
 */
object DeviceProfiles {

    val Narrow320Font2 = DeviceProfile(
        name = "Narrow320Font2",
        widthDp = 320,
        heightDp = 640,
        density = 2.0f,
        fontScale = 2.0f,
        insets = SystemInsets(top = 24, bottom = 48),
        shape = PlatformInsetShape.ANDROID,
        source = "assumed: the repo's 320 dp mockup frame at 200 percent text, a status bar and a three button bar",
    )

    val SmallPhone = DeviceProfile(
        name = "SmallPhone",
        widthDp = 360,
        heightDp = 740,
        density = 3.0f,
        insets = SystemInsets(top = 24, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "assumed: a common 360 dp phone with a status bar and a gesture bar",
    )

    val LargePhone = DeviceProfile(
        name = "LargePhone",
        widthDp = 411,
        heightDp = 891,
        density = 3.5f,
        insets = SystemInsets(top = 32, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "measured size and density, assumed insets: wm size and wm density on the Pixel 6 Pro " +
            "(1440 x 3120 at 560 dpi)",
    )

    val PhoneLandscapeCutout = DeviceProfile(
        name = "PhoneLandscapeCutout",
        widthDp = 914,
        heightDp = 411,
        density = 2.625f,
        insets = SystemInsets(left = 46, top = 28),
        shape = PlatformInsetShape.ANDROID,
        source = "measured size, density, left and top, assumed bottom: phone emulator (1080 x 2400 at 420 dpi) in " +
            "landscape, cutout and top from its window metrics in the 24 Nov 2026 QA logcat",
    )

    val AndroidLandscapeSideNavBar = DeviceProfile(
        name = "AndroidLandscapeSideNavBar",
        widthDp = 914,
        heightDp = 411,
        density = 2.625f,
        insets = SystemInsets(top = 24, right = 48),
        shape = PlatformInsetShape.ANDROID_SIDE_NAVIGATION_BAR,
        source = "assumed: a three button bar moved to the side in landscape",
    )

    val FoldCover = DeviceProfile(
        name = "FoldCover",
        widthDp = 443,
        heightDp = 995,
        density = 2.4375f,
        insets = SystemInsets(top = 28, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "measured size and density, assumed insets: fold emulator cover display " +
            "(1080 x 2424 at 390 dpi), 24 Nov 2026 QA",
    )

    val FoldInner = DeviceProfile(
        name = "FoldInner",
        widthDp = 852,
        heightDp = 883,
        density = 2.4375f,
        insets = SystemInsets(top = 28, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "measured size and density, assumed insets: fold emulator inner display " +
            "(2076 x 2152 at 390 dpi), 24 Nov 2026 QA",
    )

    val Medium720 = DeviceProfile(
        name = "Medium720",
        widthDp = 720,
        heightDp = 900,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "assumed: a window in the 600 to 839 dp band, a small tablet in portrait or an open fold, " +
            "that is single pane by the gate in docs/TABLET_FOLDABLE_UX.md",
    )

    val Gate839 = DeviceProfile(
        name = "Gate839",
        widthDp = 839,
        heightDp = 900,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "assumed: one dp under the dual pane gate (840 dp wide, 480 dp high, AdaptiveBreakpoints), " +
            "so still single pane",
    )

    val Gate840 = DeviceProfile(
        name = "Gate840",
        widthDp = 840,
        heightDp = 900,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 24),
        shape = PlatformInsetShape.ANDROID,
        source = "assumed: exactly on the dual pane gate (840 dp wide, 480 dp high, AdaptiveBreakpoints), " +
            "so the first width that is dual pane",
    )

    val TabletLandscape = DeviceProfile(
        name = "TabletLandscape",
        widthDp = 1280,
        heightDp = 800,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 48),
        shape = PlatformInsetShape.ANDROID,
        source = "measured size and density, assumed insets: tablet emulator (2560 x 1600 at 320 dpi)",
    )

    val IPhoneSmall = DeviceProfile(
        name = "IPhoneSmall",
        widthDp = 375,
        heightDp = 667,
        density = 2.0f,
        insets = SystemInsets(top = 20),
        shape = PlatformInsetShape.IOS,
        source = "assumed: an iPhone SE class screen with a status bar and no home indicator",
    )

    val IPhoneProPortrait = DeviceProfile(
        name = "IPhoneProPortrait",
        widthDp = 402,
        heightDp = 874,
        density = 3.0f,
        insets = SystemInsets(top = 62, bottom = 34),
        shape = PlatformInsetShape.IOS,
        source = "measured size, assumed insets: iPhone 18 Pro simulator screen (1206 x 2622 px at 3x), " +
            "the inset probe ran on the Duo only",
    )

    val IPhoneProLandscape = DeviceProfile(
        name = "IPhoneProLandscape",
        widthDp = 874,
        heightDp = 402,
        density = 3.0f,
        insets = SystemInsets(left = 62, right = 62, bottom = 21),
        shape = PlatformInsetShape.IOS,
        source = "assumed: the iPhone Pro turned to landscape, side strips taken to arrive as they do on the Duo",
    )

    val IPhoneDuoOuter = DeviceProfile(
        name = "IPhoneDuoOuter",
        widthDp = 466,
        heightDp = 678,
        density = 3.0f,
        insets = SystemInsets(right = 84, bottom = 34),
        shape = PlatformInsetShape.IOS,
        source = "measured: Compose inset probe on the iPhone Duo simulator, iOS 27.2, 24 Nov 2026",
    )

    val IPhoneDuoInnerPortrait = DeviceProfile(
        name = "IPhoneDuoInnerPortrait",
        widthDp = 669,
        heightDp = 951,
        density = 3.0f,
        insets = SystemInsets(right = 84, bottom = 34),
        shape = PlatformInsetShape.IOS,
        source = "assumed: a size only stand in, never a measurement. The size is the simulator display config " +
            "primary-1 of the iPhone Duo device type (2007 x 2853 px at 3x), read from its profile. The insets are " +
            "copied from the outer screen, because the inner screen cannot be driven headless to probe it",
    )

    val IPhoneDuoInnerLandscape = DeviceProfile(
        name = "IPhoneDuoInnerLandscape",
        widthDp = 951,
        heightDp = 669,
        density = 3.0f,
        insets = SystemInsets(left = 62, right = 62, bottom = 21),
        shape = PlatformInsetShape.IOS,
        source = "assumed: a size only stand in, never a measurement. The size is the simulator display config " +
            "primary-1 of the iPhone Duo device type turned to landscape. The insets are the iPhone Pro landscape " +
            "ones, because nothing is known about where the inner screen puts its strips when turned",
    )

    val IPadMiniPortrait = DeviceProfile(
        name = "IPadMiniPortrait",
        widthDp = 744,
        heightDp = 1133,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 20),
        shape = PlatformInsetShape.IOS,
        source = "measured width, assumed height and insets: the window width from docs/TABLET_FOLDABLE_UX.md " +
            "(iOS simulators, 11 Oct 2026), the height from the device's screen, a status bar and a home indicator guessed",
    )

    val IPadMiniLandscape = DeviceProfile(
        name = "IPadMiniLandscape",
        widthDp = 1133,
        heightDp = 744,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 20),
        shape = PlatformInsetShape.IOS,
        source = "measured width, assumed height and insets: the window width from docs/TABLET_FOLDABLE_UX.md " +
            "(iOS simulators, 11 Oct 2026), the height from the device's screen, a status bar and a home indicator guessed",
    )

    val IPadPro13Portrait = DeviceProfile(
        name = "IPadPro13Portrait",
        widthDp = 1032,
        heightDp = 1376,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 20),
        shape = PlatformInsetShape.IOS,
        source = "measured width, assumed height and insets: the window width from docs/TABLET_FOLDABLE_UX.md " +
            "(iOS simulators, 11 Oct 2026), the height from the device's screen, a status bar and a home indicator guessed",
    )

    val IPadPro13Landscape = DeviceProfile(
        name = "IPadPro13Landscape",
        widthDp = 1376,
        heightDp = 1032,
        density = 2.0f,
        insets = SystemInsets(top = 24, bottom = 20),
        shape = PlatformInsetShape.IOS,
        source = "measured width, assumed height and insets: the window width from docs/TABLET_FOLDABLE_UX.md " +
            "(iOS simulators, 11 Oct 2026), the height from the device's screen, a status bar and a home indicator guessed",
    )

    /** Every profile, narrowest first on Android then iOS. A matrix test runs on all of these unless it says otherwise. */
    val all: List<DeviceProfile> = listOf(
        Narrow320Font2,
        SmallPhone,
        LargePhone,
        PhoneLandscapeCutout,
        AndroidLandscapeSideNavBar,
        FoldCover,
        FoldInner,
        Medium720,
        Gate839,
        Gate840,
        TabletLandscape,
        IPhoneSmall,
        IPhoneProPortrait,
        IPhoneProLandscape,
        IPhoneDuoOuter,
        IPhoneDuoInnerPortrait,
        IPhoneDuoInnerLandscape,
        IPadMiniPortrait,
        IPadMiniLandscape,
        IPadPro13Portrait,
        IPadPro13Landscape,
    )
}
