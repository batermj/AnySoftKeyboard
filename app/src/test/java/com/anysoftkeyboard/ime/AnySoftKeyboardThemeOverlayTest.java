package com.anysoftkeyboard.ime;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.view.inputmethod.EditorInfo;

import com.anysoftkeyboard.AnySoftKeyboardBaseTest;
import com.anysoftkeyboard.AnySoftKeyboardRobolectricTestRunner;
import com.anysoftkeyboard.TestableAnySoftKeyboard;
import com.anysoftkeyboard.overlay.OverlayData;
import com.anysoftkeyboard.overlay.OverlyDataCreator;
import com.anysoftkeyboard.powersave.PowerSavingTest;
import com.anysoftkeyboard.test.SharedPrefsHelper;
import com.anysoftkeyboard.ui.settings.MainSettingsActivity;
import com.menny.android.anysoftkeyboard.R;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import androidx.test.core.app.ApplicationProvider;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class AnySoftKeyboardThemeOverlayTest extends AnySoftKeyboardBaseTest {

    private ComponentName mComponentName;
    private OverlayData mOverlayData;

    @Before
    public void setupRemoteApp() {
        mComponentName = new ComponentName("com.example", "com.example.Activity");
        final ShadowPackageManager shadowPackageManager = Shadows.shadowOf(ApplicationProvider.getApplicationContext().getPackageManager());
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mComponentName.getPackageName();
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageInfo.packageName;
        activityInfo.name = mComponentName.getClassName();
        activityInfo.enabled = true;
        activityInfo.exported = true;

        packageInfo.activities = new ActivityInfo[]{activityInfo};

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = mComponentName.getPackageName();

        shadowPackageManager.addPackage(packageInfo);

        Mockito.doReturn(new Intent().setComponent(mComponentName))
                .when(mAnySoftKeyboardUnderTest.getPackageManager())
                .getLaunchIntentForPackage(mComponentName.getPackageName());

        mOverlayData = Mockito.mock(OverlayData.class);
        Mockito.doReturn(true).when(mOverlayData).isValid();
        Mockito.doReturn(mOverlayData).when(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator()).createOverlayData(mComponentName);
    }

    @Test
    public void testDefaultAppliesInvalidOverlayAndDoesNotInteractWithCreator() {
        simulateOnStartInputFlow();
        Mockito.verifyZeroInteractions(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());

        OverlayData appliedData = captureOverlay();
        Assert.assertFalse(appliedData.isValid());
        Assert.assertNotSame(appliedData, mOverlayData);
        Assert.assertSame(AnySoftKeyboardThemeOverlay.INVALID_OVERLAY_DATA, appliedData);
    }

    @Test
    public void testWhenEnabledAppliesOverlayFromCreator() {
        Mockito.verifyZeroInteractions(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

        final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
        editorInfo.packageName = mComponentName.getPackageName();
        simulateOnStartInputFlow(false, editorInfo);
        Mockito.verify(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator()).createOverlayData(mComponentName);

        OverlayData appliedData = captureOverlay();
        Assert.assertTrue(appliedData.isValid());
        Assert.assertSame(appliedData, mOverlayData);
    }

    @Test
    public void testStartsEnabledStopsApplyingAfterDisabled() {
        Mockito.verifyZeroInteractions(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

        final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
        editorInfo.packageName = mComponentName.getPackageName();
        simulateOnStartInputFlow(false, editorInfo);

        Assert.assertSame(captureOverlay(), mOverlayData);

        simulateFinishInputFlow();

        SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, false);
        simulateOnStartInputFlow(false, editorInfo);
        Assert.assertSame(captureOverlay(), AnySoftKeyboardThemeOverlay.INVALID_OVERLAY_DATA);
    }

    @Test
    public void testAppliesInvalidIfRemotePackageDoesNotHaveIntent() {
        Mockito.verifyZeroInteractions(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

        Mockito.reset(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());

        final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
        editorInfo.packageName = "com.is.not.there";
        simulateOnStartInputFlow(false, editorInfo);
        Mockito.verifyZeroInteractions(mAnySoftKeyboardUnderTest.getMockOverlayDataCreator());

        OverlayData appliedData = captureOverlay();
        Assert.assertFalse(appliedData.isValid());
        Assert.assertSame(AnySoftKeyboardThemeOverlay.INVALID_OVERLAY_DATA, appliedData);
    }

    @Test
    public void testPowerSavingPriority() {
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_theme_control, true);
        SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

        final OverlyDataCreator originalOverlayDataCreator = mAnySoftKeyboardUnderTest.getOriginalOverlayDataCreator();

        final OverlayData normal = originalOverlayDataCreator.createOverlayData(new ComponentName(ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
        Assert.assertTrue(normal.isValid());
        Assert.assertEquals(0xFFCC99FF, normal.getPrimaryColor());
        Assert.assertEquals(0xFFAA77DD, normal.getPrimaryDarkColor());
        Assert.assertEquals(0xFF000000, normal.getPrimaryTextColor());

        PowerSavingTest.sendBatteryState(true);

        final OverlayData powerSaving = originalOverlayDataCreator.createOverlayData(new ComponentName(ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
        Assert.assertTrue(powerSaving.isValid());
        Assert.assertEquals(0xFF000000, powerSaving.getPrimaryColor());
        Assert.assertEquals(0xFF000000, powerSaving.getPrimaryDarkColor());
        Assert.assertEquals(0xFF888888, powerSaving.getPrimaryTextColor());
    }

    private OverlayData captureOverlay() {
        return captureOverlay(mAnySoftKeyboardUnderTest);
    }

    public static OverlayData captureOverlay(TestableAnySoftKeyboard testableAnySoftKeyboard) {
        ArgumentCaptor<OverlayData> captor = ArgumentCaptor.forClass(OverlayData.class);
        Mockito.verify(testableAnySoftKeyboard.getInputView(), Mockito.atLeastOnce()).setKeyboardOverlay(captor.capture());

        return captor.getValue();
    }
}