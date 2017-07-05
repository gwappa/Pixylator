SRCDIR=src src_IJ
BINDIR=classes
MANI=manifest
VPATH+=${BINDIR} ${BINDIR}/lab/proj/chaos/colortrack ${SRCDIR}

JRE=/Applications/ImageJ/jre/lib/rt.jar
IJ=/Applications/ImageJ/ImageJ.app/Contents/Java/ij.jar
CP=${BINDIR}:.:vio.jar:javacpp.jar:
BOOT=-source 1.6 -target 1.6 -Xbootclasspath/p:${JRE} -Xbootclasspath/p:${IJ}
OUTDIR=-d ${BINDIR}
OPT=-cp ${CP} -Xlint -Xlint:-path -Xdiags:verbose
JAVAC=javac ${BOOT} ${OUTDIR} ${OPT}

ROOTCLASS=Pixylator_alpha.class
UICONTROLS=HistogramControl.class ROIControl.class FrameControl.class
CALC=CentroidCalculator.class CMCalculator.class
MEASOUT=NoMeasurementOutput.class ResultsTableOutput.class CSVOutput.class
MASKOUT=NoMaskOutput.class ImageStackOutput.class H264MaskOutput.class
JARFILE=colortrack.jar
BUILDDIR=Pixylator
ZIPFILE=Pixylator_build.zip

${JARFILE}:
	jar cvfm $@ ${MANI} -C ${BINDIR} lab

build: ${JARFILE} extra
	mkdir build/${BUILDDIR}
	cp *.jar *.class build/${BUILDDIR}
	zip -rv build/${ZIPFILE} build/${BUILDDIR}
	rm -rf build/${BUILDDIR}

%.class: %.java
	${JAVAC} $<

${JARFILE}: ImageSelector.class HistogramGeneration.class Pixylation.class
ImageSelector.class: PixylatorDirectives.class ImageSelectionListener.class IJLogger.class
HistogramGeneration.class: ImageSelectionListener.class ParameterListener.class ActionDelegate.class HueHistogram.class ${UICONTROLS}
Pixylation.class: ImageSelectionListener.class ParameterListener.class HueMaskControl.class ${UICONTROLS}
Pixylation.class: Measurement.class MeasurementOutput.class MaskOutput.class Hue.class Luma.class IJLogger.class

${JARFILE}: ActionDelegate.class FrameControl.class ROIControl.class OutputControl.class ${CALC}
${JARFILE}: HistogramControl.class HueHistogram.class HueMaskControl.class
${JARFILE}: MeasurementControl.class Hue.class Luma.class MaskOutput.class TrackingListener.class
${JARFILE}: Measurement.class MeasurementOutput.class MeasurementControl.class
${JARFILE}: FileNameFunctions.class

ActionDelegate.class:
FrameControl.class: ActionDelegate.class ParameterNotifier.class ParameterModel.class
ROIControl.class: ActionDelegate.class ParameterNotifier.class ParameterModel.class
HistogramControl.class: ActionDelegate.class ParameterModel.class ParameterNotifier.class
HueHistogram.class: Hue.class
HueMaskControl.class:
OutputControl.class: OutputSelector.class MaskOutput.class MeasurementOutput.class ${MEASOUT} ${MASKOUT}
OutputSelector.class: TrackingListener.class
MeasuermentControl.class: Measurement.class
Hue.class:
${CALC}: AbstractMeasurement.class MeasurementOutput.class
${MEASOUT}: MeasurementOutput.class IJLogger.class
${MASKOUT}: MaskOutput.class IJLogger.class
ResultsTableOutput.class: FileNameFunctions.class
ImageStackOutput.class: FileNameFunctions.class
H264MaskOutput.class: FileNameFunctions.class
AbstractMeasurement.class: Measurement.class MeasurementOutput.class
Measurement.class: TrackingListener.class MeasurementOutput.class
MeasurementOutput.class: TrackingListener.class
MaskOutput.class: TrackingListener.class
TrackingListener.class:

ParameterNotifier.class: ParameterModel.class ParameterListener.class
ParameterModel.class: ParameterListener.class
ParameterListener.class:

extra: ${JARFILE}
	javac ${BOOT} ${OPT} Pixylator_alpha.java Virtual_H264.java
clean:
	rm -rf ${BINDIR}/* *.class

distclean: clean
	rm -f ${JARFILE}
