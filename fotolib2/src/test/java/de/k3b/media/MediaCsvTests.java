/*
 * Copyright (c) 2016-2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.media;

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import de.k3b.csv2db.csv.CsvLoader;
import de.k3b.TestUtil;
import de.k3b.io.DateUtil;

/**
 * Created by k3b on 11.10.2016.
 */

public class MediaCsvTests {

    public static String createTestCsv(int... ids) {
        StringWriter result = new StringWriter();
        MediaCsvSaver saver = new MediaCsvSaver(new PrintWriter(result));
        for (int id : ids) {
            MediaDTO item = TestUtil.createTestMediaDTO(id);
            saver.save(item);
        }
        return result.toString();
    }

    private static class Sut extends CsvLoader<MediaCsvItem> {
        private ArrayList<IMetaApi> result = new ArrayList<IMetaApi>();

        @Override
        protected void onNextItem(MediaCsvItem next, int lineNumber, int recordNumber) {
            if (next != null) {
                result.add(new MediaDTO(next));
            }
        }

        protected List<IMetaApi> load(int... ids) {
            String data = createTestCsv(ids);
            return load(data);
        }

        protected List<IMetaApi> load(String data) {
            result.clear();
            super.load(TestUtil.createReader(data), new MediaCsvItem());
            return result;
        }
    }

    @Test
    public void shouldLoad1() {
        TimeZone.setDefault(DateUtil.UTC);
        Sut sut = new Sut();
        List<IMetaApi> actual = sut.load(1);
        MediaDTO expected = TestUtil.createTestMediaDTO(1);
        Assert.assertEquals(expected.toString(), actual.get(0).toString());
    }

    @Test
    public void shouldLoadExtremas() {
        String csv = "a;" + MediaXmpFieldDefinition.title.getShortName() + ";c\n"
                + "normal;#1;regular\n"
                + "short;#2\n"
                + "long;#3;something;extra column\n"
                + "empty\n"
                + "quoted;\"#5\";regular\n";
        Sut sut = new Sut();
        List<IMetaApi> actual = sut.load(csv);
        Assert.assertEquals("#", 5, actual.size());
        Assert.assertEquals("unquote", "#5", actual.get(4).getTitle());
    }

    @Test
    public void shouldLoadRealData() {
        String csv = "SourceFile,About,ActiveD-Lighting,AdvancedSceneMode,AdvancedSceneType,AEBBracketValue,AELockButton,AESetting,AF-CPrioritySelection,AF-OnForMB-D11,AF-SPrioritySelection,AFAperture,AFAreaHeight,AFAreaHeights,AFAreaMode,AFAreaWidth,AFAreaWidths,AFAreaXPositions,AFAreaYPositions,AFAssist,AFAssistLamp,AFFineTune,AFFineTuneAdj,AFFineTuneIndex,AFIlluminator,AFImageHeight,AFImageWidth,AFInfo2Version,AFPoint,AFPointIllumination,AFPointPosition,AFPointSelected,AFPointSet,AFPointsInFocus,AFPointsUsed,AFResponse,AFResult,AlreadyApplied,Anti-Blur,Aperture,ApertureValue,APP14Flags0,APP14Flags1,ApplicationRecordVersion,ApproximateFocusDistance,Artist,AspectRatio,Audio,AutoBracketing,AutoBracketOrder,AutoBracketSet,AutoBrightness,AutoDistortionControl,AutoExposureBracketing,AutoFocus,AutoISO,AutoLateralCA,AutoRotate,AutoWhiteVersion,AuxiliaryLens,BabyAge,BaseISO,BatteryOrder,BeepPitch,BeepVolume,BitsPerSample,BlackLevel,Blacks2012,BlueBalance,BlueHue,BlueMatrixColumn,BlueSaturation,BlueTRC,BlurWarning,Brightness,BrightnessValue,BulbDuration,BurstMode,BWMode,By-line,CameraDateTime,CameraID,CameraISO,CameraProfile,CameraTemperature,CameraType,CanonExposureMode,CanonFirmwareVersion,CanonFlashMode,CanonImageHeight,CanonImageSize,CanonImageType,CanonImageWidth,CanonModelID,Caption-Abstract,Categories,CategoriesIconResource,CategoriesName,CategoriesNameTranslationMode,CategoriesSmartSearchSupport,CategoriesUIOrder,CategoriesUIVisibility,Category,CenterWeightedAreaSize,CFAPattern,ChromaticAberrationB,ChromaticAberrationR,ChromaticAdaptation,CircGradBasedCorrActive,CircGradBasedCorrAmount,CircGradBasedCorrBrightness,CircGradBasedCorrClarity2012,CircGradBasedCorrContrast2012,CircGradBasedCorrDefringe,CircGradBasedCorrExposure2012,CircGradBasedCorrHighlights2012,CircGradBasedCorrHue,CircGradBasedCorrLuminanceNoise,CircGradBasedCorrMaskAngle,CircGradBasedCorrMaskBottom,CircGradBasedCorrMaskFeather,CircGradBasedCorrMaskFlipped,CircGradBasedCorrMaskLeft,CircGradBasedCorrMaskMidpoint,CircGradBasedCorrMaskRight,CircGradBasedCorrMaskRoundness,CircGradBasedCorrMaskTop,CircGradBasedCorrMaskValue,CircGradBasedCorrMaskVersion,CircGradBasedCorrMaskWhat,CircGradBasedCorrMoire,CircGradBasedCorrSaturation,CircGradBasedCorrShadows2012,CircGradBasedCorrSharpness,CircGradBasedCorrTemperature,CircGradBasedCorrTint,CircGradBasedCorrWhat,CircleOfConfusion,City,Clarity2012,CLModeShootingSpeed,CMMFlags,CodedCharacterSet,CodePage,ColorComponents,ColorControl,ColorEffect,ColorHue,ColorMatrix,ColorMode,ColorNoiseReduction,ColorNoiseReductionDetail,ColorNoiseReductionSmoothness,ColorSpace,ColorSpaceData,ColorTemperature,ColorTransform,CommandDialsApertureSetting,CommandDialsChangeMainSub,CommandDialsMenuAndPlayback,CommandDialsReverseRotation,Comment,ComponentsConfiguration,CompressedBitsPerPixel,Compression,ConditionalFEC,ConnectionSpaceIlluminant,ContinuousDrive,Contrast,Contrast2012,ContrastDetectAF,ContrastDetectAFInFocus,ContrastMode,ControlMode,ConversionLens,ConvertToGrayscale,Copyright,CopyrightFlag,CopyrightNotice,CoringFilter,Country,Country-PrimaryLocationName,CreateDate,CreatingApplication,CreativeStyle,Creator,CreatorAddress,CreatorCity,CreatorPostalCode,CreatorRegion,CreatorTool,CropAngle,CropBottom,CropConstrainToWarp,CropHiSpeed,CropLeft,CroppedImageHeight,CroppedImageLeft,CroppedImageTop,CroppedImageWidth,CropRight,CropTop,CurrentIPTCDigest,CustomRendered,DataDump,Date,DateAcquired,DateCreated,DateDisplayFormat,DateStampMode,DateTimeCreated,DateTimeDigitized,DateTimeOriginal,DaylightSavings,DCTEncodeVersion,DefringeGreenAmount,DefringeGreenHueHi,DefringeGreenHueLo,DefringePurpleAmount,DefringePurpleHueHi,DefringePurpleHueLo,DeletedImageCount,DependentImage1EntryNumber,DependentImage2EntryNumber,DerivedFromDocumentID,DerivedFromInstanceID,DerivedFromOriginalDocumentID,Description,DestinationCity,DestinationDST,DeviceAttributes,DeviceManufacturer,DeviceMfgDesc,DeviceModel,DeviceModelDesc,DeviceSettingDescription,DigitalCreationDate,DigitalCreationDateTime,DigitalCreationTime,DigitalZoom,DigitalZoomRatio,Directory,DirectoryNumber,DisplayAperture,DisplayedUnitsX,DisplayedUnitsY,DocumentAncestors,DocumentID,DOF,DriveMode,DSPFirmwareVersion,EasyExposureCompensation,EasyMode,EffectiveMaxAperture,EncodingProcess,EnvelopeRecordVersion,ExifByteOrder,ExifImageHeight,ExifImageWidth,ExifToolVersion,ExifVersion,ExitPupilPosition,Exposure,Exposure2012,ExposureBracketValue,ExposureCompensation,ExposureControlStep,ExposureCount,ExposureDelayMode,ExposureDifference,ExposureIndex,ExposureMode,ExposureProgram,ExposureTime,ExposureTuning,ExposureWarning,ExtensionClassID,ExtensionCreateDate,ExtensionDescription,ExtensionModifyDate,ExtensionName,ExtensionPersistence,ExternalFlashBounce,ExternalFlashCompensation,ExternalFlashExposureComp,ExternalFlashFirmware,ExternalFlashFlags,ExternalFlashGValue,ExternalFlashMode,ExternalFlashZoom,FaceDetectFrameSize,FacePositions,FacesDetected,FaceWidth,FileAccessDate,FileCreateDate,FileInfoVersion,FileModifyDate,FileName,FileNumber,FileNumberSequence,FilePermissions,FileSize,FileSource,FileType,FilterEffect,Firmware,FirmwareRevision,FirmwareVersion,Flash,FlashActivity,FlashBias,FlashBits,FlashChargeLevel,FlashColorFilter,FlashCommanderMode,FlashCompensation,FlashControlBuilt-in,FlashControlMode,FlashDevice,FlashExposureBracketValue,FlashExposureComp,FlashExposureComp3,FlashExposureComp4,FlashFired,FlashFunction,FlashGNDistance,FlashGroupACompensation,FlashGroupAControlMode,FlashGroupBCompensation,FlashGroupBControlMode,FlashGroupCCompensation,FlashGroupCControlMode,FlashGuideNumber,FlashInfoVersion,FlashLevel,FlashMode,FlashOutput,FlashpixVersion,FlashRedEyeMode,FlashReturn,FlashSetting,FlashShutterSpeed,FlashSource,FlashSyncSpeed,FlashType,FlashWarning,FNumber,FocalLength,FocalLength35efl,FocalLengthIn35mmFormat,FocalPlaneDiagonal,FocalPlaneResolutionUnit,FocalPlaneXResolution,FocalPlaneXSize,FocalPlaneYResolution,FocalPlaneYSize,FocalType,FocalUnits,FocusContinuous,FocusDistance,FocusDistanceLower,FocusDistanceUpper,FocusMode,FocusPixel,FocusPointWrap,FocusPosition,FocusRange,FocusStepCount,FocusStepInfinity,FocusStepNear,FocusTrackingLockOn,FocusWarning,Format,FOV,FrameNumber,FujiFlashMode,FuncButton,GainControl,Gamma,GlobalAltitude,GlobalAngle,GPSAltitude,GPSAltitudeRef,GPSDateStamp,GPSDateTime,GPSDestBearingRef,GPSDestDistanceRef,GPSImgDirection,GPSImgDirectionRef,GPSLatitude,GPSLatitudeRef,GPSLongitude,GPSLongitudeRef,GPSPosition,GPSProcessingMethod,GPSSpeedRef,GPSTimeStamp,GPSTrackRef,GPSVersionID,GradientBasedCorrActive,GradientBasedCorrAmount,GradientBasedCorrBrightness,GradientBasedCorrClarity2012,GradientBasedCorrContrast2012,GradientBasedCorrDefringe,GradientBasedCorrExposure2012,GradientBasedCorrHighlights2012,GradientBasedCorrHue,GradientBasedCorrLuminanceNoise,GradientBasedCorrMaskFullX,GradientBasedCorrMaskFullY,GradientBasedCorrMaskValue,GradientBasedCorrMaskWhat,GradientBasedCorrMaskZeroX,GradientBasedCorrMaskZeroY,GradientBasedCorrMoire,GradientBasedCorrSaturation,GradientBasedCorrShadows2012,GradientBasedCorrSharpness,GradientBasedCorrTemperature,GradientBasedCorrTint,GradientBasedCorrWhat,GrainAmount,GrayMixerAqua,GrayMixerBlue,GrayMixerGreen,GrayMixerMagenta,GrayMixerOrange,GrayMixerPurple,GrayMixerRed,GrayMixerYellow,GreenHue,GreenMatrixColumn,GreenSaturation,GreenTRC,GridDisplay,HasCrop,HasExtendedXMP,HasSettings,Headline,HierarchicalSubject,HighISONoiseReduction,Highlights2012,History,HistoryAction,HistoryChanged,HistoryInstanceID,HistoryParameters,HistorySoftwareAgent,HistoryWhen,HometownCity,HometownDST,HueAdjustment,HueAdjustmentAqua,HueAdjustmentBlue,HueAdjustmentGreen,HueAdjustmentMagenta,HueAdjustmentOrange,HueAdjustmentPurple,HueAdjustmentRed,HueAdjustmentYellow,HyperfocalDistance,ICCProfileName,ImageAdjustment,ImageBoundary,ImageCount,ImageDataSize,ImageDescription,ImageEditCount,ImageHeight,ImageNumber,ImageProcessing,ImageQuality,ImageReviewTime,ImageSize,ImageStabilization,ImageUniqueID,ImageWidth,InstanceID,IntelligentContrast,InternalFlashTable,InternalSerialNumber,InteropIndex,InteropVersion,IPTCDigest,ISO,ISO2,ISODisplay,ISOExpansion,ISOExpansion2,ISOSelection,ISOSensitivityStep,ISOSetting,ISOSpeed,ISOValue,JFIFVersion,JPEGQuality,Keywords,LCDIllumination,LegacyIPTCDigest,Lens,Lens35efl,LensDataVersion,LensDistortionParams,LensFStops,LensID,LensIDNumber,LensInfo,LensManualDistortionAmount,LensModel,LensProfileChromaticAberrationScale,LensProfileDigest,LensProfileDistortionScale,LensProfileEnable,LensProfileFilename,LensProfileName,LensProfileSetup,LensProfileVignettingScale,LensSerialNumber,LensSpec,LensType,LightCondition,LightReading,LightSource,LightValue,LightValueCenter,LightValuePeriphery,LiveViewAFAreaMode,LiveViewAFMode,LiveViewMonitorOffTime,Location,Luminance,LuminanceAdjustmentAqua,LuminanceAdjustmentBlue,LuminanceAdjustmentGreen,LuminanceAdjustmentMagenta,LuminanceAdjustmentOrange,LuminanceAdjustmentPurple,LuminanceAdjustmentRed,LuminanceAdjustmentYellow,LuminanceNoiseReductionContrast,LuminanceNoiseReductionDetail,LuminanceSmoothing,Macro,MacroMode,Make,MakerNoteVersion,ManualFlashOutput,ManualFocusDistance,Marked,MaxAperture,MaxApertureAtMaxFocal,MaxApertureAtMinFocal,MaxApertureValue,MaxContinuousRelease,MaxFocalLength,MB-D11BatteryType,MCUVersion,MeasuredEV,MeasurementBacking,MeasurementFlare,MeasurementGeometry,MeasurementIlluminant,MeasurementObserver,MediaBlackPoint,MediaWhitePoint,MenuMonitorOffTime,MetadataDate,MeteringMode,MeteringTime,MIMEType,MinAperture,MinFocalLength,Model,ModelingFlash,ModifyDate,MPFVersion,MPImageFlags,MPImageFormat,MPImageLength,MPImageStart,MPImageType,MultiExposureAutoGain,MultiExposureMode,MultiExposureShots,MultiExposureVersion,MyColorMode,NativeDigest,NDFilter,NoiseReduction,NoMemoryCard,NumAFPoints,NumberOfFocusPoints,NumberOfImages,NumFacePositions,ObjectName,OffsetSchema,OKButton,OlympusImageHeight,OlympusImageWidth,OpticalZoomCode,OpticalZoomMode,Orientation,OriginalDocumentID,OriginatingProgram,OtherImageLength,OtherImageStart,OwnerName,Padding,PaintCorrectionActive,PaintCorrectionAmount,PaintCorrectionBrightness,PaintCorrectionClarity2012,PaintCorrectionContrast2012,PaintCorrectionDefringe,PaintCorrectionExposure2012,PaintCorrectionHighlights2012,PaintCorrectionHue,PaintCorrectionLuminanceNoise,PaintCorrectionMaskCenterWeight,PaintCorrectionMaskDabs,PaintCorrectionMaskFlow,PaintCorrectionMaskRadius,PaintCorrectionMaskValue,PaintCorrectionMaskWhat,PaintCorrectionMoire,PaintCorrectionSaturation,PaintCorrectionShadows2012,PaintCorrectionSharpness,PaintCorrectionTemperature,PaintCorrectionTint,PaintCorrectionWhat,PanasonicExifVersion,ParametricDarks,ParametricHighlights,ParametricHighlightSplit,ParametricLights,ParametricMidtoneSplit,ParametricShadows,ParametricShadowSplit,PentaxImageSize,PentaxModelID,PentaxModelType,PerspectiveAspect,PerspectiveHorizontal,PerspectiveRotate,PerspectiveScale,PerspectiveUpright,PerspectiveVertical,PhaseDetectAF,PhotoEffect,PhotometricInterpretation,PhotoshopBGRThumbnail,PhotoshopFormat,PhotoshopQuality,PhotoshopThumbnail,PictureControlAdjust,PictureControlBase,PictureControlName,PictureControlQuickAdjust,PictureControlVersion,PictureMode,PlanarConfiguration,PlaybackMonitorOffTime,PostCropVignetteAmount,PowerUpTime,PreviewButton,PreviewImage,PreviewImageHeight,PreviewImageLength,PreviewImageSize,PreviewImageStart,PreviewImageWidth,PrimaryAFPoint,PrimaryChromaticities,PrimaryPlatform,PrintIMVersion,ProcessingSoftware,ProcessVersion,ProfileClass,ProfileCMMType,ProfileConnectionSpace,ProfileCopyright,ProfileCreator,ProfileDateTime,ProfileDescription,ProfileDescriptionML,ProfileDescriptionML-da-DK,ProfileDescriptionML-de-DE,ProfileDescriptionML-es-ES,ProfileDescriptionML-fi-FI,ProfileDescriptionML-fr-FU,ProfileDescriptionML-it-IT,ProfileDescriptionML-ja-JP,ProfileDescriptionML-ko-KR,ProfileDescriptionML-nl-NL,ProfileDescriptionML-no-NO,ProfileDescriptionML-pt-BR,ProfileDescriptionML-sv-SE,ProfileDescriptionML-zh-CN,ProfileDescriptionML-zh-TW,ProfileFileSignature,ProfileID,ProfileVersion,ProgramISO,ProgramMode,ProgramShift,ProgramVersion,ProgressiveScans,Province-State,Quality,Rating,RatingPercent,RawFileName,RecommendedExposureIndex,RecordMode,RedBalance,RedEyeReduction,RedHue,RedMatrixColumn,RedSaturation,RedTRC,RegionAppliedToDimensionsH,RegionAppliedToDimensionsUnit,RegionAppliedToDimensionsW,RegionAreaH,RegionAreaUnit,RegionAreaW,RegionAreaX,RegionAreaY,RegionName,RegionType,RelatedImageHeight,RelatedImageWidth,RelatedSoundFile,ReleaseButtonToUseDial,ReleaseMode,RemoteOnDuration,RenderingIntent,ResolutionUnit,RetouchAreaFeather,RetouchAreaMaskAlpha,RetouchAreaMaskCenterValue,RetouchAreaMaskPerimeterValue,RetouchAreaMaskSizeX,RetouchAreaMaskSizeY,RetouchAreaMaskValue,RetouchAreaMaskWhat,RetouchAreaMaskX,RetouchAreaMaskY,RetouchAreaMethod,RetouchAreaOffsetY,RetouchAreaOpacity,RetouchAreaSeed,RetouchAreaSourceState,RetouchAreaSourceX,RetouchAreaSpotType,RetouchHistory,RetouchInfo,ReverseIndicators,Rights,Rotation,SamplesPerPixel,Saturation,SaturationAdjustmentAqua,SaturationAdjustmentBlue,SaturationAdjustmentGreen,SaturationAdjustmentMagenta,SaturationAdjustmentOrange,SaturationAdjustmentPurple,SaturationAdjustmentRed,SaturationAdjustmentYellow,ScaleFactor35efl,SceneAssist,SceneCaptureType,SceneDetect,SceneMode,SceneType,ScreenNail,ScreenTips,SelfTimer,SelfTimer2,SelfTimerInterval,SelfTimerShotCount,SelfTimerTime,SensingMethod,SensitivityType,SensorPixelSize,SequenceNumber,SerialNumber,Shadows,Shadows2012,ShadowTint,SharpenDetail,SharpenEdgeMasking,SharpenRadius,Sharpness,SharpnessFactor,ShootingInfoDisplay,ShootingInfoMonitorOffTime,ShootingMode,ShotInfoVersion,ShutterCount,ShutterCurtainHack,ShutterReleaseButtonAE-L,ShutterSpeed,ShutterSpeedValue,SlowShutter,SlowSync,Software,Source,SpecialMode,SplitToningBalance,SplitToningHighlightHue,SplitToningHighlightSaturation,SplitToningShadowHue,SplitToningShadowSaturation,SpotMeteringMode,State,Storage-StreamPathname,Sub-location,Subject,SubjectArea,SubjectDistance,SubjectDistanceRange,SubSecCreateDate,SubSecDateTimeOriginal,SubSecModifyDate,SubSecTime,SubSecTimeDigitized,SubSecTimeOriginal,TargetAperture,TargetExposureTime,Technology,TextStamp,ThumbnailImage,ThumbnailImageValidArea,ThumbnailLength,ThumbnailOffset,Time,TimeCreated,TimeSincePowerOn,Timezone,Tint,Title,ToneComp,ToneCurve,ToneCurveBlue,ToneCurveGreen,ToneCurveName,ToneCurveName2012,ToneCurvePV2012,ToneCurvePV2012Blue,ToneCurvePV2012Green,ToneCurvePV2012Red,ToneCurveRed,ToningEffect,ToningSaturation,TravelDay,UprightCenterMode,UprightCenterNormX,UprightCenterNormY,UprightDependentDigest,UprightFocalLength35mm,UprightFocalMode,UprightPreview,UprightTransform_0,UprightTransform_1,UprightTransform_2,UprightTransform_3,UprightTransform_4,UprightTransformCount,UprightVersion,Urgency,URL,UsedExtensionNumbers,UserComment,ValidAFPoints,ValidBits,VariProgram,Version,Vibrance,VibrationReduction,ViewfinderWarning,ViewingCondDesc,ViewingCondIlluminant,ViewingCondIlluminantType,ViewingCondSurround,VignetteAmount,VRDOffset,VRInfoVersion,VRMode,Warning,WB_GRBGLevels,WB_RBLevels,WB_RGGBLevels,WBBlueLevel,WBGreenLevel,WBMode,WBRedLevel,WhiteBalance,WhiteBalanceBias,WhiteBalanceFineTune,WhitePoint,Whites2012,WorldTimeLocation,XMPToolkit,XPAuthor,XPComment,XPKeywords,XPSubject,XPTitle,XResolution,YCbCrCoefficients,YCbCrPositioning,YCbCrSubSampling,YResolution,ZoomSourceWidth,ZoomStepCount,ZoomTargetWidth\n" +
                "./02-06Greve/020305blockland/ClaudiaNebenFahrrad.jpg,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,4,,,,,,,,,,,,,,,,,,,,,,8,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,UTF8,,3,,,,,,,,,Uncalibrated,,,,,,,,,\"Y, Cb, Cr, -\",,,,,,,,,,,,,,,,,,,,2002:03:05 23:09:04,,,,,,,,,,,,,,,,,,,,b443520a10119da99c2550175e6d0efb,,,,,,,,,,2002:03:05 23:09:04,,,,,,,,,,,,,,,,,,,,,,,,,,,,,./02-06Greve/020305blockland,,,,,,,,,,,,,\"Baseline DCT, Huffman coding\",4,\"Big-endian (Motorola, MM)\",480,640,9.74,0220,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,2002:03:05 23:09:04+01:00,2013:07:31 19:21:39+02:00,,2002:03:05 23:09:04+01:00,ClaudiaNebenFahrrad.jpg,,,rw-rw-rw-,49 kB,,JPEG,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0100,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,480,,,,,640x480,,,640,,,,,,,b443520a10119da99c2550175e6d0efb,,,,,,,,,,,1.01,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,image/jpeg,,,,,2013:06:14 09:35:16,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,480,pixel,640,0.266667,normalized,0.16875,0.496875,0.141667,ClaudiaGreve,Face,,,,,,,,inches,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Picasa,,,,,,,,,,,,ClaudiaGreve,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Image::ExifTool 6.93,,,,,,72,,Centered,YCbCr4:2:0 (2 2),72,,,\n" +
                "./02-06Greve/020305blockland/ClaudiaNebenFahrrad.xmp,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,2002:03:05 23:09:04,,,,,,,,,,,,,,,,,,,,,,,,,2002-03-05,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,./02-06Greve/020305blockland,,,,,,,,,,,,,,,,,,9.74,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,2013:07:31 20:34:38+02:00,2013:07:31 20:34:38+02:00,,2013:07:31 20:34:38+02:00,ClaudiaNebenFahrrad.xmp,,,rw-rw-rw-,713 bytes,,XMP,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,application/rdf+xml,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\"ClaudiaGreve, Greve\",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Image::ExifTool 9.33,,,,,,,,,,,,,\n";
        Sut sut = new Sut();
        List<IMetaApi> actual = sut.load(csv);
        for(IMetaApi item : actual) {
            System.out.println(item.toString());
        }

    }


    @Test
    public void shouldSaveLoadExtra() {
        String data = new MediaAsString().setExtra("some extra").toString();
        MediaAsString sut = new MediaAsString().fromString(data);
        Assert.assertEquals("some extra", sut.getExtra());
    }

    @Test
    public void shouldCreateCsv() {
        String csv = createTestCsv(1,2);
        System.out.println(csv);

    }
}
