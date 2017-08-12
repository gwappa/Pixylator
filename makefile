
# change the following pointers before running 'make'

RT=../../jre/lib/rt.jar
IJ=../../ImageJ.app/Contents/Java/ij.jar

#
# then run 'make plugin' or 'make rebuild' to build everything
#

SRCDIR=src src_IJ
BUILDDIR=Pixylator
BINDIR=classes
MANI=manifest
VPATH+=${BINDIR} ${BINDIR}/lab/proj/chaos/colortrack ${SRCDIR} ${BUILDDIR}

CP=${BINDIR}:.:vio.jar:javacpp.jar
BOOT=${RT}:${IJ}
OUTDIR=-d ${BINDIR}
OPT=-bootclasspath ${BOOT} -cp ${CP} -Xlint -Xlint:-path -Xdiags:verbose
JAVAC=javac ${OUTDIR} ${OPT}

ROOTCLASS=Pixylator_beta.class
UICONTROLS=HistogramControl.class ROIControl.class FrameControl.class
CALC=CentroidCalculator.class CMCalculator.class
MEASOUT=NoMeasurementOutput.class ResultsTableOutput.class CSVOutput.class
MASKOUT=NoMaskOutput.class ImageStackOutput.class H264MaskOutput.class
JARFILE=colortrack.jar
TARBALL=Pixylator_build.tar.gz

${JARFILE}:
	jar cvfm $@ ${MANI} -C ${BINDIR} lab

build: ${TARBALL}
${TARBALL}: ${JARFILE} plugin
	mkdir build/${BUILDDIR}
	cp *.jar *.class build/${BUILDDIR}
	cd build && tar zcvf ${TARBALL} ${BUILDDIR}/*
	rm -rf build/${BUILDDIR}

${BINDIR}:
	mkdir $@

%.class: %.java ${BINDIR}
	${JAVAC} $<

${JARFILE}: ImageSelector.class HistogramGeneration.class Pixylation.class
ImageSelector.class: PixylatorDirectives.class ImageSelectionListener.class IJLogger.class
HistogramGeneration.class: ImageSelectionListener.class ParameterListener.class ActionDelegate.class HueHistogram.class ${UICONTROLS}
Pixylation.class: ImageSelectionListener.class ParameterListener.class HueMaskControl.class ${UICONTROLS}
Pixylation.class: Measurement.class MeasurementOutput.class MaskOutput.class Hue.class Luma.class IJLogger.class

${JARFILE}: ActionDelegate.class FrameControl.class ROIControl.class OutputControl.class ${CALC}
${JARFILE}: HistogramControl.class HueHistogram.class HueMaskControl.class
${JARFILE}: MeasurementControl.class Hue.class Luma.class MaskOutput.class TrackerElement.class
${JARFILE}: Measurement.class MeasurementOutput.class MeasurementControl.class
${JARFILE}: FileNameFunctions.class

ActionDelegate.class:
FrameControl.class: ActionDelegate.class ParameterNotifier.class ParameterModel.class
ROIControl.class: ActionDelegate.class ParameterNotifier.class ParameterModel.class
HistogramControl.class: ActionDelegate.class ParameterModel.class ParameterNotifier.class
HueHistogram.class: Hue.class
HueMaskControl.class:
OutputControl.class: OutputSelector.class MaskOutput.class MeasurementOutput.class ${MEASOUT} ${MASKOUT}
OutputSelector.class: TrackerElement.class
MeasuermentControl.class: Measurement.class
Hue.class:
${CALC}: AbstractMeasurement.class MeasurementOutput.class
${MEASOUT}: MeasurementOutput.class IJLogger.class
${MASKOUT}: MaskOutput.class IJLogger.class
ResultsTableOutput.class: FileNameFunctions.class
ImageStackOutput.class: FileNameFunctions.class
H264MaskOutput.class: FileNameFunctions.class
AbstractMeasurement.class: Measurement.class MeasurementOutput.class
Measurement.class: TrackerElement.class MeasurementOutput.class
MeasurementOutput.class: TrackerElement.class
MaskOutput.class: TrackerElement.class
TrackerElement.class:

ParameterNotifier.class: ParameterModel.class ParameterListener.class
ParameterModel.class: ParameterListener.class
ParameterListener.class:

plugin: ${JARFILE}
	javac ${OPT} Virtual_H264.java Pixylator_beta.java
clean:
	rm -rf ${BINDIR}/* *.class

distclean: clean
	rm -f ${JARFILE}
