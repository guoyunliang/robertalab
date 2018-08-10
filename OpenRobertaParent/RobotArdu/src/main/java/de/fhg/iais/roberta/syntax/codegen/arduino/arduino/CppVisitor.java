package de.fhg.iais.roberta.syntax.codegen.arduino.arduino;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.fhg.iais.roberta.components.ConfigurationBlockType;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.components.UsedConfigurationBlock;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.components.arduino.ArduinoConfiguration;
import de.fhg.iais.roberta.mode.action.MotorMoveMode;
import de.fhg.iais.roberta.mode.sensor.HumiditySensorMode;
import de.fhg.iais.roberta.mode.sensor.InfraredSensorMode;
import de.fhg.iais.roberta.mode.sensor.RfidSensorMode;
import de.fhg.iais.roberta.syntax.BlocklyConstants;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.action.control.RelayAction;
import de.fhg.iais.roberta.syntax.action.display.ClearDisplayAction;
import de.fhg.iais.roberta.syntax.action.display.ShowPictureAction;
import de.fhg.iais.roberta.syntax.action.display.ShowTextAction;
import de.fhg.iais.roberta.syntax.action.light.LightAction;
import de.fhg.iais.roberta.syntax.action.light.LightStatusAction;
import de.fhg.iais.roberta.syntax.action.motor.CurveAction;
import de.fhg.iais.roberta.syntax.action.motor.DriveAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorDriveStopAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorGetPowerAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorOnAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorSetPowerAction;
import de.fhg.iais.roberta.syntax.action.motor.MotorStopAction;
import de.fhg.iais.roberta.syntax.action.motor.TurnAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayFileAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayNoteAction;
import de.fhg.iais.roberta.syntax.action.sound.SayTextAction;
import de.fhg.iais.roberta.syntax.action.sound.SetLanguageAction;
import de.fhg.iais.roberta.syntax.action.sound.ToneAction;
import de.fhg.iais.roberta.syntax.action.sound.VolumeAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.ExternalLedOffAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.ExternalLedOnAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.LedOffAction;
import de.fhg.iais.roberta.syntax.actors.arduino.mbot.LedOnAction;
import de.fhg.iais.roberta.syntax.check.hardware.arduino.arduino.UsedHardwareCollectorVisitor;
import de.fhg.iais.roberta.syntax.codegen.arduino.ArduinoVisitor;
import de.fhg.iais.roberta.syntax.lang.blocksequence.MainTask;
import de.fhg.iais.roberta.syntax.sensor.generic.BrickSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.DropSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.EncoderSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.HumiditySensor;
import de.fhg.iais.roberta.syntax.sensor.generic.InfraredSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.LightSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.MoistureSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.MotionSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.PulseSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.RfidSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.TemperatureSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.UltrasonicSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.VoltageSensor;
import de.fhg.iais.roberta.util.dbc.DbcException;
import de.fhg.iais.roberta.visitor.AstVisitor;
import de.fhg.iais.roberta.visitors.arduino.ArduinoAstVisitor;

/**
 * This class is implementing {@link AstVisitor}. All methods are implemented and they append a human-readable C representation of a phrase to a
 * StringBuilder. <b>This representation is correct C code for Arduino.</b> <br>
 */
public class CppVisitor extends ArduinoVisitor implements ArduinoAstVisitor<Void> {
    private final boolean isTimerSensorUsed;

    /**
     * Initialize the C++ code generator visitor.
     *
     * @param programPhrases to generate the code from
     * @param indentation to start with. Will be incr/decr depending on block structure
     */
    private CppVisitor(ArduinoConfiguration brickConfiguration, ArrayList<ArrayList<Phrase<Void>>> phrases, int indentation) {
        super(phrases, indentation);
        UsedHardwareCollectorVisitor codePreprocessVisitor = new UsedHardwareCollectorVisitor(phrases, brickConfiguration);
        this.usedSensors = codePreprocessVisitor.getUsedSensors();
        this.usedVars = codePreprocessVisitor.getVisitedVars();
        this.usedConfigurationBlocks = codePreprocessVisitor.getUsedConfigurationBlocks();
        //TODO: fix how the timer is detected for all robots
        this.isTimerSensorUsed = codePreprocessVisitor.isTimerSensorUsed();
        this.loopsLabels = codePreprocessVisitor.getloopsLabelContainer();
    }

    /**
     * factory method to generate C++ code from an AST.<br>
     *
     * @param brickConfiguration
     * @param programPhrases to generate the code from
     * @param withWrapping if false the generated code will be without the surrounding configuration code
     */
    public static String generate(ArduinoConfiguration brickConfiguration, ArrayList<ArrayList<Phrase<Void>>> programPhrases, boolean withWrapping) {
        CppVisitor astVisitor = new CppVisitor(brickConfiguration, programPhrases, withWrapping ? 1 : 0);
        astVisitor.generateCode(withWrapping);
        return astVisitor.sb.toString();
    }

    @Override
    public Void visitShowPictureAction(ShowPictureAction<Void> showPictureAction) {
        return null;
    }

    @Override
    public Void visitShowTextAction(ShowTextAction<Void> showTextAction) {
        this.sb.append("lcd_" + showTextAction.getPort().getOraName() + ".setCursor(");
        showTextAction.getX().visit(this);
        this.sb.append(",");
        showTextAction.getY().visit(this);
        this.sb.append(");");
        nlIndent();
        this.sb.append("lcd_" + showTextAction.getPort().getOraName() + ".print(");
        showTextAction.getMsg().visit(this);
        this.sb.append(");");
        return null;
    }

    @Override
    public Void visitClearDisplayAction(ClearDisplayAction<Void> clearDisplayAction) {
        this.sb.append("lcd_" + clearDisplayAction.getPort().getOraName() + ".clear();");
        return null;
    }

    @Override
    public Void visitLedOffAction(LedOffAction<Void> ledOffAction) {
        return null;
    }

    @Override
    public Void visitVolumeAction(VolumeAction<Void> volumeAction) {
        return null;
    }

    @Override
    public Void visitSetLanguageAction(SetLanguageAction<Void> setLanguageAction) {
        return null;
    }

    @Override
    public Void visitSayTextAction(SayTextAction<Void> sayTextAction) {
        return null;
    }

    @Override
    public Void visitLightAction(LightAction<Void> lightAction) {
        if ( !lightAction.getMode().toString().equals(BlocklyConstants.DEFAULT) ) {
            this.sb.append("digitalWrite(_led_" + lightAction.getPort().getOraName() + ", " + lightAction.getMode().getValues()[0] + ");");
        } else {
            String in = lightAction.getRgbLedColor().toString();
            String regexString = Pattern.quote("NumConst [") + "(.*?)" + Pattern.quote("],");
            Pattern p = Pattern.compile(regexString);
            Matcher m = p.matcher(in);
            String[] colors = {
                "red",
                "green",
                "blue"
            };
            int color_index = 0;
            while ( m.find() ) {
                this.sb.append("analogWrite(_led_" + colors[color_index] + "_" + lightAction.getPort().getOraName() + ", " + m.group(1) + ");");
                nlIndent();
                color_index++;
            }
        }
        return null;
    }

    @Override
    public Void visitLightStatusAction(LightStatusAction<Void> lightStatusAction) {
        String[] colors = {
            "red",
            "green",
            "blue"
        };
        for ( int i = 0; i < 3; i++ ) {
            this.sb.append("analogWrite(_led_" + colors[i] + "_" + lightStatusAction.getPort().getOraName() + ", 0);");
            nlIndent();
        }
        return null;
    }

    @Override
    public Void visitPlayFileAction(PlayFileAction<Void> playFileAction) {
        return null;
    }

    @Override
    public Void visitToneAction(ToneAction<Void> toneAction) {
        //9 - sound port
        this.sb.append("tone(_spiele_" + toneAction.getPort().getOraName() + ",");
        toneAction.getFrequency().visit(this);
        this.sb.append(", ");
        toneAction.getDuration().visit(this);
        this.sb.append(");");
        return null;
    }

    @Override
    public Void visitPlayNoteAction(PlayNoteAction<Void> playNoteAction) {
        return null;
    }

    @Override
    public Void visitMotorOnAction(MotorOnAction<Void> motorOnAction) {
        boolean step = motorOnAction.getParam().getDuration() != null;
        if ( step ) {//step motor
            this.sb.append("Motor_" + motorOnAction.getPort().getOraName() + ".setSpeed(");
            motorOnAction.getParam().getSpeed().visit(this);
            this.sb.append(");");
            nlIndent();
            this.sb.append("Motor_" + motorOnAction.getPort().getOraName() + ".step(_SPU_" + motorOnAction.getPort().getOraName() + "*");
            motorOnAction.getDurationValue().visit(this);
            if ( motorOnAction.getDurationMode().equals(MotorMoveMode.DEGREE) ) {
                this.sb.append("/360");
            }
            this.sb.append(");");
        } else {//servo motor
            this.sb.append("_servo_" + motorOnAction.getPort().getOraName() + ".write(");
            motorOnAction.getParam().getSpeed().visit(this);
            this.sb.append(");");
        }
        return null;
    }

    @Override
    public Void visitRelayAction(RelayAction<Void> relayAction) {
        this.sb
            .append("digitalWrite(_relay_")
            .append(relayAction.getPort().getOraName())
            .append(", ")
            .append(relayAction.getMode().getValues()[0])
            .append(");");
        return null;
    }

    @Override
    public Void visitMotorSetPowerAction(MotorSetPowerAction<Void> motorSetPowerAction) {
        return null;
    }

    @Override
    public Void visitMotorGetPowerAction(MotorGetPowerAction<Void> motorGetPowerAction) {
        return null;
    }

    @Override
    public Void visitMotorStopAction(MotorStopAction<Void> motorStopAction) {
        return null;
    }

    @Override
    public Void visitDriveAction(DriveAction<Void> driveAction) {
        return null;
    }

    @Override
    public Void visitCurveAction(CurveAction<Void> curveAction) {
        return null;
    }

    @Override
    public Void visitTurnAction(TurnAction<Void> turnAction) {
        return null;
    }

    @Override
    public Void visitMotorDriveStopAction(MotorDriveStopAction<Void> stopAction) {
        return null;
    }

    @Override
    public Void visitLightSensor(LightSensor<Void> lightSensor) {
        this.sb.append("analogRead(_output_" + lightSensor.getPort().getOraName() + ")*100/1024");
        return null;
    }

    @Override
    public Void visitBrickSensor(BrickSensor<Void> button) {
        this.sb.append("digitalRead(_taster_" + button.getPort().getOraName() + ")  == HIGH");
        return null;
    }

    public void measureDistanceUltrasonicSensor(String sensorName) {
        this.sb.append("double _getUltrasonicDistance()\n{");
        nlIndent();
        this.sb.append("digitalWrite(_trigger_" + sensorName + ", LOW);");
        nlIndent();
        this.sb.append("delay(5);");
        nlIndent();
        this.sb.append("digitalWrite(_trigger_" + sensorName + ", HIGH);");
        nlIndent();
        this.sb.append("delay(10);");
        nlIndent();
        this.sb.append("digitalWrite(_trigger_" + sensorName + ", LOW);");
        nlIndent();
        this.sb.append("return pulseIn(_echo_" + sensorName + ", HIGH)*_signalToDistance;");
        this.decrIndentation();
        this.sb.append("\n}");
    }

    @Override
    public Void visitUltrasonicSensor(UltrasonicSensor<Void> ultrasonicSensor) {
        this.sb.append("_getUltrasonicDistance()");
        return null;
    }

    @Override
    public Void visitMoistureSensor(MoistureSensor<Void> moistureSensor) {
        this.sb.append("analogRead(_moisturePin_" + moistureSensor.getPort().getOraName() + ")*100/1024");
        return null;
    }

    @Override
    public Void visitTemperatureSensor(TemperatureSensor<Void> temperatureSensor) {
        this.sb.append("map(analogRead(_TMP36_" + temperatureSensor.getPort().getOraName() + "), 0, 410, -50, 150)");
        return null;
    }

    @Override
    public Void visitEncoderSensor(EncoderSensor<Void> encoderSensor) {
        this.sb.append("meinEncoder.read()");
        return null;
    }

    @Override
    public Void visitVoltageSensor(VoltageSensor<Void> potentiometer) {
        this.sb.append("((double)analogRead(_output_" + potentiometer.getPort().getOraName() + "))*5/1024");
        return null;
    }

    @Override
    public Void visitHumiditySensor(HumiditySensor<Void> humiditySensor) {
        switch ( (HumiditySensorMode) humiditySensor.getMode() ) {
            case HUMIDITY:
                this.sb.append("_dht_" + humiditySensor.getPort().getOraName() + ".readHumidity()");
                break;
            case TEMPERATURE:
                this.sb.append("_dht_" + humiditySensor.getPort().getOraName() + ".readTemperature()");
                break;
            default:
                throw new DbcException("Invalide mode for Humidity Sensor!");
        }
        return null;
    }

    @Override
    public Void visitDropSensor(DropSensor<Void> dropSensor) {
        this.sb.append("analogRead(_S_" + dropSensor.getPort().getOraName() + ")");
        return null;
    }

    @Override
    public Void visitPulseSensor(PulseSensor<Void> pulseSensor) {
        this.sb.append("analogRead(_SensorPin_" + pulseSensor.getPort().getOraName() + ")");
        return null;
    }

    @Override
    public Void visitRfidSensor(RfidSensor<Void> rfidSensor) {
        switch ( (RfidSensorMode) rfidSensor.getMode() ) {
            case PRESENCE:
                this.sb.append("mfrc522_" + rfidSensor.getPort().getCodeName() + ".PICC_IsNewCardPresent()");
                break;
            case SERIAL:
                this.sb.append(
                    "String(((long)(mfrc522_"
                        + rfidSensor.getPort().getCodeName()
                        + ".uid.uidByte[0])<<24) |((long)(mfrc522_"
                        + rfidSensor.getPort().getCodeName()
                        + ".uid.uidByte[1])<<16) | ((long)(mfrc522_"
                        + rfidSensor.getPort().getCodeName()
                        + ".uid.uidByte[2])<<8) | ((long)mfrc522_"
                        + rfidSensor.getPort().getCodeName()
                        + ".uid.uidByte[3]), HEX)");
                break;
            default:
                throw new DbcException("Invalide mode for RFID Sensor!");
        }
        return null;
    }

    public void measureIRValue(UsedSensor infraredSensor) {
        switch ( (InfraredSensorMode) infraredSensor.getMode() ) {
            case PRESENCE:
                this.sb.append("bool _getIRResults()\n{");
                incrIndentation();
                nlIndent();
                this.sb.append("bool results = false;");
                nlIndent();
                this.sb.append("if (_irrecv_" + infraredSensor.getPort().getOraName() + ".decode(&_results_" + infraredSensor.getPort().getOraName() + ")) {");
                incrIndentation();
                nlIndent();
                this.sb.append("results = true;");
                nlIndent();
                this.sb.append("_irrecv_" + infraredSensor.getPort().getOraName() + ".resume();");
                decrIndentation();
                nlIndent();
                this.sb.append("}");
                break;
            case VALUE:
                this.sb.append("long int _getIRResults()\n{");
                incrIndentation();
                nlIndent();
                this.sb.append("long int results = 0;");
                nlIndent();
                this.sb.append("if (_irrecv_" + infraredSensor.getPort().getOraName() + ".decode(&_results_" + infraredSensor.getPort().getOraName() + ")) {");
                incrIndentation();
                nlIndent();
                this.sb.append("results = _results_" + infraredSensor.getPort().getOraName() + ".value;");
                nlIndent();
                this.sb.append("_irrecv_" + infraredSensor.getPort().getOraName() + ".resume();");
                decrIndentation();
                nlIndent();
                this.sb.append("}");
                break;
            default:
                throw new DbcException("Invalide mode for IR Sensor!");
        }
        nlIndent();
        this.sb.append("return results;");
        decrIndentation();
        this.sb.append("\n}");
    }

    @Override
    public Void visitInfraredSensor(InfraredSensor<Void> infraredSensor) {
        this.sb.append("_getIRResults()");
        return null;
    }

    @Override
    public Void visitMotionSensor(MotionSensor<Void> motionSensor) {
        this.sb.append("digitalRead(_output_" + motionSensor.getPort().getOraName() + ") == HIGH");
        return null;
    }

    @Override
    public Void visitMainTask(MainTask<Void> mainTask) {
        decrIndentation();
        mainTask.getVariables().visit(this);
        nlIndent();
        generateConfigurationVariables();
        if ( this.isTimerSensorUsed ) {
            nlIndent();
            this.sb.append("unsigned long __time = millis(); \n");
        }
        generateUserDefinedMethods();
        for ( UsedSensor usedSensor : this.usedSensors ) {
            if ( usedSensor.getType().equals(SensorType.INFRARED) ) {
                this.sb.append("\n");
                this.measureIRValue(usedSensor);
                break;
            }
        }
        for ( UsedConfigurationBlock usedConfigurationBlock : this.usedConfigurationBlocks ) {
            if ( usedConfigurationBlock.getType().equals(ConfigurationBlockType.ULTRASONIC) ) {
                this.sb.append("\n");
                this.measureDistanceUltrasonicSensor(usedConfigurationBlock.getBlockName());
                break;
            }
        }
        this.sb.append("\n \nvoid setup() \n");
        this.sb.append("{");
        incrIndentation();
        nlIndent();
        this.sb.append("Serial.begin(9600); ");
        nlIndent();
        this.generateConfigurationSetup();
        generateUsedVars();
        this.sb.append("\n}\n");
        this.sb.append("\n").append("void loop() \n");
        this.sb.append("{");
        return null;
    }

    @Override
    protected void generateProgramPrefix(boolean withWrapping) {
        if ( !withWrapping ) {
            return;
        }
        this.sb.append("#include <math.h> \n");
        for ( UsedConfigurationBlock usedConfigurationBlock : this.usedConfigurationBlocks ) {
            switch ( (ConfigurationBlockType) usedConfigurationBlock.getType() ) {
                case HUMIDITY:
                    this.sb.append("#include <DHT.h> \n");
                    break;
                case INFRARED:
                    this.sb.append("#include <IRremote.h> \n");
                    break;
                case ENCODER:
                    this.sb.append("#include <Encoder.h>  \n");
                    break;
                case RFID:
                    this.sb.append("#include <SPI.h> \n");
                    this.sb.append("#include <MFRC522.h> \n");
                    break;
                case LCD:
                    this.sb.append("#include <LiquidCrystal.h> \n");
                    break;
                case LCDI2C:
                    this.sb.append("#include <LiquidCrystal_I2C.h> \n");
                    break;
                case STEPMOTOR:
                    this.sb.append("#include <Stepper.h> \n");
                    break;
                case SERVOMOTOR:
                    this.sb.append("#include <Servo.h> \n");
                    break;
                case ULTRASONIC:
                case MOTION:
                case MOISTURE:
                case KEY:
                case LIGHT:
                case POTENTIOMETER:
                case TEMPERATURE:
                case DROP:
                case PULSE:
                case LED:
                case RGBLED:
                case BUZZER:
                case RELAY:
                    break;
                default:
                    throw new DbcException("Sensor is not supported: " + usedConfigurationBlock.getType());
            }
        }
        this.sb.append("#include <RobertaFunctions.h>   // Open Roberta library \n");
        this.sb.append("RobertaFunctions rob;  \n");
    }

    @Override
    protected void generateProgramSuffix(boolean withWrapping) {
        if ( withWrapping ) {
            this.sb.append("\n}\n");
        }
    }

    private void generateConfigurationSetup() {
        for ( UsedConfigurationBlock usedConfigurationBlock : this.usedConfigurationBlocks ) {
            switch ( (ConfigurationBlockType) usedConfigurationBlock.getType() ) {
                case HUMIDITY:
                    this.sb.append("_dht_" + usedConfigurationBlock.getBlockName() + ".begin();");
                    nlIndent();
                    break;
                case ULTRASONIC:
                    this.sb.append("pinMode(_trigger_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    this.sb.append("pinMode(_echo_" + usedConfigurationBlock.getBlockName() + ", INPUT);");
                    nlIndent();
                    break;
                case MOTION:
                    this.sb.append("pinMode(_output_" + usedConfigurationBlock.getBlockName() + ", INPUT);");
                    nlIndent();
                    break;
                case MOISTURE:
                    break;
                case INFRARED:
                    this.sb.append("pinMode(13, OUTPUT);");
                    nlIndent();
                    this.sb.append("_irrecv_" + usedConfigurationBlock.getBlockName() + ".enableIRIn();");
                    nlIndent();
                    break;
                case KEY:
                    this.sb.append("pinMode(_taster_" + usedConfigurationBlock.getBlockName() + ", INPUT);");
                    nlIndent();
                case LIGHT:
                    break;
                case POTENTIOMETER:
                    break;
                case TEMPERATURE:
                    break;
                case ENCODER:
                    this.sb.append("pinMode(_SW_" + usedConfigurationBlock.getBlockName() + ", INPUT);");
                    nlIndent();
                    this.sb.append("attachInterrupt(digitalPinToInterrupt(_SW_" + usedConfigurationBlock.getBlockName() + "), Interrupt, CHANGE);");
                    nlIndent();
                    break;
                case DROP:
                    break;
                case PULSE:
                    break;
                case RFID:
                    this.sb.append("SPI.begin();");
                    nlIndent();
                    this.sb.append("_mfrc522_" + usedConfigurationBlock.getBlockName() + ".PCD_Init();");
                    nlIndent();
                    break;
                case LCD:
                    this.sb.append("lcd_" + usedConfigurationBlock.getBlockName() + ".begin(16, 2);");
                    nlIndent();
                    break;
                case LCDI2C:
                    this.sb.append("lcd_" + usedConfigurationBlock.getBlockName() + ".begin();");
                    nlIndent();
                    break;
                case LED:
                    this.sb.append("pinMode(_led_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    break;
                case RGBLED:
                    this.sb.append("pinMode(_led_red_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    this.sb.append("pinMode(_led_green_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    this.sb.append("pinMode(_led_blue_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    break;
                case BUZZER:
                    break;
                case RELAY:
                    this.sb.append("pinMode(_relay_" + usedConfigurationBlock.getBlockName() + ", OUTPUT);");
                    nlIndent();
                    break;
                case STEPMOTOR:
                    break;
                case SERVOMOTOR:
                    this.sb.append("_servo_" + usedConfigurationBlock.getBlockName() + ".attach(" + usedConfigurationBlock.getPins().get(0) + ");");
                    nlIndent();
                    break;
                default:
                    throw new DbcException("Sensor is not supported: " + usedConfigurationBlock.getType());
            }
        }
    }

    private void generateConfigurationVariables() {
        for ( UsedConfigurationBlock usedConfigurationBlock : this.usedConfigurationBlocks ) {
            String blockName = usedConfigurationBlock.getBlockName();
            switch ( (ConfigurationBlockType) usedConfigurationBlock.getType() ) {
                case HUMIDITY:
                    this.sb.append("#define DHTPIN" + blockName + " ").append(usedConfigurationBlock.getPins().get(0));
                    nlIndent();
                    this.sb.append("#define DHTTYPE DHT11");
                    nlIndent();
                    this.sb.append("DHT _dht_" + blockName + "(DHTPIN" + blockName + ", DHTTYPE);");
                    nlIndent();
                    break;
                case ULTRASONIC:
                    this.sb.append("int _trigger_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    this.sb.append("int _echo_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(1)).append(";");
                    nlIndent();
                    this.sb.append(
                        "double _signalToDistance = 0.03432/2; //Nun berechnet man die Entfernung in Zentimetern. Man teilt zunächst die Zeit durch zwei (Weil man ja nur eine Strecke berechnen möchte und nicht die Strecke hin- und zurück). Den Wert multipliziert man mit der Schallgeschwindigkeit in der Einheit Zentimeter/Mikrosekunde und erhält dann den Wert in Zentimetern.");
                    nlIndent();
                    break;
                case MOISTURE:
                    this.sb.append("int _moisturePin_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case INFRARED:
                    this.sb.append("int _RECV_PIN_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    this.sb.append("IRrecv _irrecv_" + blockName + "(_RECV_PIN_" + blockName + ");");
                    nlIndent();
                    this.sb.append("decode_results _results_" + blockName + ";");
                    nlIndent();
                    break;
                case LIGHT:
                    this.sb.append("int _output_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case MOTION:
                    this.sb.append("int _output_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case POTENTIOMETER:
                    this.sb.append("int _output_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case TEMPERATURE:
                    this.sb.append("int _TMP36_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case ENCODER:
                    this.sb.append("int _CLK_" + blockName + " = 6;");
                    nlIndent();
                    this.sb.append("int _DT_" + blockName + " = 5;");
                    nlIndent();
                    this.sb.append("int _SW_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    this.sb.append("Encoder _myEncoder_" + blockName + "(_DT_" + blockName + ", _CLK_" + blockName + ");");
                    nlIndent();
                    break;
                case DROP:
                    this.sb.append("int _S_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case PULSE:
                    this.sb.append("int _SensorPin_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case RFID:
                    this.sb.append("#define SS_PIN_" + blockName + " " + usedConfigurationBlock.getPins().get(0));
                    nlIndent();
                    this.sb.append("#define RST_PIN_" + blockName + " " + usedConfigurationBlock.getPins().get(1));
                    nlIndent();
                    this.sb.append("MFRC522 _mfrc522_" + blockName + "(SS_PIN_" + blockName + ", RST_PIN_" + blockName + ");");
                    nlIndent();
                    break;
                case KEY:
                    this.sb.append("int _taster_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case LCD:
                    this.sb
                        .append("LiquidCrystal lcd_" + blockName + "(")
                        .append(usedConfigurationBlock.getPins().get(0))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(1))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(2))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(3))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(4))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(5))
                        .append(");");
                    nlIndent();
                    break;
                case LCDI2C:
                    this.sb.append("LiquidCrystal_I2C lcd_" + blockName + "(0x27, 16, 2);");
                    nlIndent();
                    break;
                case LED:
                    this.sb.append("int _led_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case RGBLED:
                    this.sb.append("int _led_red_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    this.sb.append("int _led_green_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(1)).append(";");
                    nlIndent();
                    this.sb.append("int _led_blue_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(2)).append(";");
                    nlIndent();
                    break;
                case BUZZER:
                    this.sb.append("int _spiele_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case RELAY:
                    this.sb.append("int _relay_" + blockName + " = ").append(usedConfigurationBlock.getPins().get(0)).append(";");
                    nlIndent();
                    break;
                case STEPMOTOR:
                    this.sb.append("int _SPU_" + blockName + " = ").append("2048;"); //TODO: change 2048 to customized
                    nlIndent();
                    this.sb
                        .append("Stepper Motor_" + blockName + "(_SPU_" + blockName + ", ")
                        .append(usedConfigurationBlock.getPins().get(0))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(1))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(2))
                        .append(", ")
                        .append(usedConfigurationBlock.getPins().get(3))
                        .append(");");
                    nlIndent();
                    break;
                case SERVOMOTOR:
                    this.sb.append("Servo _servo_" + blockName + ";");
                    nlIndent();
                    break;
                default:
                    throw new DbcException("Configuration block is not supported: " + usedConfigurationBlock.getType());
            }
        }
    }

    @Override
    public Void visitExternalLedOnAction(ExternalLedOnAction<Void> externalLedOnAction) {
        return null;
    }

    @Override
    public Void visitExternalLedOffAction(ExternalLedOffAction<Void> externalLedOffAction) {
        return null;
    }

    @Override
    public Void visitLedOnAction(LedOnAction<Void> ledOnAction) {
        return null;
    }
}
