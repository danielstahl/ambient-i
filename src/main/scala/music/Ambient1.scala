package music

import music.ARControlInstrumentBuilder._
import music.LineControlInstrumentBuilder._
import music.SineControlInstrumentBuilder._
import net.soundmining.Instrument._
import net.soundmining.Utils._
import net.soundmining.{BusGenerator, InstrumentBuilder, MusicPlayer, Spectrum}

/**
 * First ambient
  *
  *
  * Basically you have three "ambient" moment with
  * prologue/transition/epilogue. These transitions
  * serves as "doors" into/between/exit of the ambient
  * movements. The ambient parts stand still and
  * the other parts have a movement and a direction.
  * They will be between 15-30 second long and the
  * ambient parts will be about 3 minutes.
  *
  * prologue, pulse
  * ambient I, noise
  * transition I, sine
  * ambient II, pulse
  * transition II, noise
  * ambient III, sine
  * epilogue
  *
 */
object Ambient1 {

  val spectrum = Spectrum.makeSpectrum(20, 1, 200)
  val invertedSpectrum = Spectrum.makeInvertedSpectrum(20, 1, 200)

  def playPulse(start: Float, dur: Float, freq: Float, amp: Float = 0.2f)(implicit player: MusicPlayer): Unit = {
    val pulse = new  PulseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(0)
      .dur(dur)
      .freqBus.control(line(dur, freq, freq))
      .widthBus.control(line(dur, 0.1f, 0.9f))
      .ampBus.control(ar(dur, 0.5f, (0f, amp, 0f)))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse)
  }

  def playSine(start: Float, dur: Float, freq: Float)(implicit player: MusicPlayer): Unit = {
    val pulse = new  SineInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(0)
      .dur(dur)
      .freqBus.control(line(dur, freq, freq))
      .ampBus.control(ar(dur, 0.5f, (0f, 0.2f, 0f)))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse)
  }

  def playSubtractiveChord(start: Float, dur: Float, attack: Float, freqs: Seq[(Float, Float)], amp: Float = 0.2f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val noise = new WhiteNoiseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(ar(dur, attack, (0f, amp, 0f)))
      .buildInstruments()

    val filters = freqs.flatMap {
      case (freq, attackPoint) =>
        new FilterReplaceInstrumentBuilder()
          .addAction(TAIL_ACTION)
          .in(bus)
          .dur(dur)
          .ampBus.control(ar(dur, attackPoint, (0f, amp, 0f)))
          .bwBus.control(line(dur, 0.00000001f, 0.000000001f))
          .freqBus.control(line(dur, freq, freq))
          .buildInstruments()
    }

    val volume = new MonoVolumeBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .out(0)
      .dur(dur)
      .ampBus.control(line(dur, 1f, 1f))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), noise ++ filters ++ volume)
  }
/*
  def test1(): Unit = {
    val spectrum = Spectrum.makeSpectrum(20, 1, 200)

    BusGenerator.reset()
    implicit val player: MusicPlayer = MusicPlayer()

    player.startPlay()

    setupNodes(player)

    playPulse(0f, 10f, spectrum(3), amp = 0.02f)
    playPulse(0f, 10f, spectrum(10), amp = 0.02f)
    playSine(3f, 10f, spectrum(12))
    playSine(6f, 10f, spectrum(14))

    playSine(10f, 10f, spectrum(15))
    playPulse(13f, 10f, spectrum(16), amp = 0.02f)
    playSine(16f, 10f, spectrum(17))

    playSine(20f, 10f, spectrum(20))
    playSine(23f, 10f, spectrum(23))
    playPulse(26f, 10f, spectrum(26), amp = 0.02f)


    playSubtractiveChord(0, 10, 0.5f, Seq((spectrum(30), 0.3f), (spectrum(32), 0.5f), (spectrum(34), 0.7f)), 0.8f)
    playSubtractiveChord(10, 10, 0.5f, Seq((spectrum(35), 0.3f), (spectrum(36), 0.5f), (spectrum(37), 0.7f)), 0.8f, 17)
    playSubtractiveChord(20, 10, 0.5f, Seq((spectrum(46), 0.3f), (spectrum(43), 0.5f), (spectrum(46), 0.7f)), 0.8f, 18)

    playSine(10f, 10f, spectrum(15))
    playPulse(13f, 10f, spectrum(16), amp = 0.02f)
    playSine(16f, 10f, spectrum(17))

    playSine(20f, 10f, spectrum(20))
    playSine(23f, 10f, spectrum(23))
    playPulse(26f, 10f, spectrum(26), amp = 0.02f)


    Thread.sleep(5000)
  }
*/

  def whiteHighpass(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), dustSpeed: (Float, Float, Float), dustAmpFactor: Float = 20, panSpeed: (Float, Float), panPhase: Float = 0f, filterFreq: (Float, Float), filterAdd: (Float, Float), filterMul: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    assert(filterAdd._1 >= filterMul._1)
    assert(filterAdd._2 >= filterMul._2)
    val highpass = new HighpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(sin(dur, startFreq = filterFreq._1, endFreq = filterFreq._2, addStart = filterAdd._1, addEnd = filterAdd._2, mulStart = filterMul._1, mulEnd = filterMul._2))
    white(start, dur, amp, ampSpeed, dustSpeed, dustAmpFactor, panSpeed, panPhase, highpass, bus)

  }

  def whiteLowpass(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), dustSpeed: (Float, Float, Float), dustAmpFactor: Float = 20, panSpeed: (Float, Float), panPhase: Float = 0f, filterFreq: (Float, Float), filterAdd: (Float, Float), filterMul: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    assert(filterAdd._1 >= filterMul._1)
    assert(filterAdd._2 >= filterMul._2)
    val lowpass = new LowpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(sin(dur, startFreq = filterFreq._1, endFreq = filterFreq._2, addStart = filterAdd._1, addEnd = filterAdd._2, mulStart = filterMul._1, mulEnd = filterMul._2))
    white(start, dur, amp, ampSpeed, dustSpeed, dustAmpFactor, panSpeed, panPhase, lowpass, bus)
  }

  def white(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), dustSpeed: (Float, Float, Float), dustAmpFactor: Float = 20, panSpeed: (Float, Float), panPhase: Float = 0f, filter: InstrumentBuilder, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val noise = new WhiteNoiseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val dust = new DustInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1 * dustAmpFactor, mulEnd = amp._2 * dustAmpFactor))
      .freqBus.control(ar(dur, 0.5f, (dustSpeed._1, dustSpeed._2, dustSpeed._3)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), noise ++ dust ++ filter.buildInstruments() ++ pan)
  }

  def pink(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), dustSpeed: (Float, Float, Float), dustAmpFactor: Float = 20, panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val pink = new PinkNoiseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val dust = new DustInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1 * dustAmpFactor, mulEnd = amp._2 * dustAmpFactor))
      .freqBus.control(ar(dur, 0.5f, (dustSpeed._1, dustSpeed._2, dustSpeed._3)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pink ++ dust ++  pan)
  }

  def brown(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), dustSpeed: (Float, Float, Float), dustAmpFactor: Float = 20, panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val brown = new BrownNoiseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val dust = new DustInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1 * dustAmpFactor, mulEnd = amp._2 * dustAmpFactor))
      .freqBus.control(ar(dur, 0.5f, (dustSpeed._1, dustSpeed._2, dustSpeed._3)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), brown ++ dust ++  pan)
  }



  def pulse(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), freq: Float, panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val pulse = new  PulseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freq, freq))
      .widthBus.control(line(dur, 0.1f, 0.9f))
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse ++ pan)
  }

  def slowPulseLowpass(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), freq: (Float, Float), width: (Float, Float), filterFreq: (Float, Float), panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val pulse = new  PulseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freq._1, freq._2))
      .widthBus.control(line(dur, width._1, width._2))
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val lowpass = new LowpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(line(dur, filterFreq._1, filterFreq._2))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse ++ lowpass ++ pan)
  }

  def slowPulseHighpass(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), freq: (Float, Float), width: (Float, Float), filterFreq: (Float, Float), panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val pulse = new  PulseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freq._1, freq._2))
      .widthBus.control(line(dur, width._1, width._2))
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val highpass = new HighpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(line(dur, filterFreq._1, filterFreq._2))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse ++ highpass ++ pan)
  }

  def sine(start: Float, dur: Float, amp: (Float, Float) = (0.2f, 0.2f), ampSpeed: (Float, Float), freq: Float, panSpeed: (Float, Float), panPhase: Float = 0f, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val sine = new SineInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freq, freq))
      .ampBus.control(sin(dur, ampSpeed._1/dur, ampSpeed._2/dur, mulStart = amp._1, mulEnd = amp._2))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(sin(dur, panSpeed._1/dur, panSpeed._2/dur, phase = panPhase))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), sine ++ pan)
  }

  def ambient1noise(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = spectrum(8)
    println(s"Ambient1, noise. Duration: $dur")

    whiteLowpass(start = start, dur = dur,
      amp = (0.04f, 0.01f), ampSpeed = (3f, 2f),
      dustSpeed = (2, 10, 5), dustAmpFactor = 25,
      panSpeed = (5f, 3f),
      filterFreq = (3f/dur, 5f/dur), filterAdd = (1000f, 4000f), filterMul = (100f, 1000f),
      bus = 16)

    whiteHighpass(start = start, dur = dur,
      amp = (0.01f, 0.04f), ampSpeed = (5f, 3f),
      dustSpeed = (5, 15, 3), dustAmpFactor = 25,
      panSpeed = (2f, 5f), panPhase = Math.PI.toFloat,
      filterFreq = (5f/dur, 3f/dur), filterAdd = (4000f, 1000f), filterMul = (1000f, 100f),
      bus = 17)

    whiteLowpass(start = start, dur = dur,
      amp = (0.03f, 0.04f), ampSpeed = (5f, 3f),
      dustSpeed = (13, 8, 3), dustAmpFactor = 25,
      panSpeed = (5f, 8f),
      filterFreq = (3f/dur, 5f/dur), filterAdd = (4000f, 1000f), filterMul = (1000f, 500f),
      bus = 18)

    whiteHighpass(start = start, dur = dur,
      amp = (0.04f, 0.03f), ampSpeed = (3f, 5f),
      dustSpeed = (10, 16, 6), dustAmpFactor = 25,
      panSpeed = (7f, 4f), panPhase = Math.PI.toFloat,
      filterFreq = (8f/dur, 13f/dur), filterAdd = (1000f, 3000f), filterMul = (1000f, 2000f),
      bus = 19)

    pink(start = start, dur = dur,
      amp = (0.05f, 0.08f), ampSpeed = (2f, 1f),
      dustSpeed = (3, 8, 5), dustAmpFactor = 10,
      panSpeed = (1f, 3f),
      bus = 20)

    pink(start = start, dur = dur,
      amp = (0.05f, 0.08f), ampSpeed = (2f, 1f),
      dustSpeed = (21, 3, 13), dustAmpFactor = 10,
      panSpeed = (5f, 3f), panPhase = Math.PI.toFloat,
      bus = 21)

    brown(start = start, dur = dur,
      amp = (0.04f, 0.03f), ampSpeed = (1f, 2f),
      dustSpeed = (8, 5, 3), dustAmpFactor = 10,
      panSpeed = (3f, 1f),  panPhase = Math.PI.toFloat,
      bus = 22)

    brown(start = start, dur = dur,
      amp = (0.04f, 0.03f), ampSpeed = (2f, 5f),
      dustSpeed = (8, 5, 3), dustAmpFactor = 10,
      panSpeed = (1f, 5f),
      bus = 23)
  }

  def ambient2pulse(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = spectrum(7)
    println(s"Ambient2, pulse. Duration: $dur")

    slowPulseLowpass(start, dur = dur, amp = (0.002f, 0.004f), ampSpeed = (2f, 1f), freq = (0.1f, 0.2f), width = (0.01f, 0.5f), filterFreq = (200f, 1000f), panSpeed = (3f, 2f), bus = 24)
    pulse(start, dur = dur, amp = (0.04f, 0.04f), ampSpeed = (2f, 1f), freq = spectrum(3), panSpeed = (3f, 2f), bus = 25)
    pulse(start, dur = dur, amp = (0.05f, 0.05f), ampSpeed = (1f, 2f), freq = spectrum(0), panSpeed = (5f, 8f), bus = 26)
    pulse(start, dur = dur, amp = (0.03f, 0.03f), ampSpeed = (5f, 3f), freq = spectrum(1), panSpeed = (21f, 13f), bus = 27)
    pulse(start, dur = dur, amp = (0.02f, 0.02f), ampSpeed = (3f, 5f), freq = spectrum(2), panSpeed = (8f, 13f), bus = 28)
    slowPulseHighpass(start, dur = dur, amp = (0.12f, 0.08f), ampSpeed = (1f, 2f), freq = (0.3f, 0.5f), width = (0.25f, 0.75f), filterFreq = (4000f, 3000f), panSpeed = (5f, 8f), bus = 29)
  }

  def ambient3sine(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = spectrum(9)

    println(s"Ambient3, sine. Duration: $dur")

    sine(start, dur = dur, amp = (0.06f, 0.04f), ampSpeed = (1f, 2f), freq = spectrum(2), panSpeed = (5f, 8f), bus = 30)
    sine(start, dur = dur, amp = (0.04f, 0.05f), ampSpeed = (3f, 1f), freq = spectrum(4), panSpeed = (3f, 13f), bus = 31)
    sine(start, dur = dur, amp = (0.03f, 0.02f), ampSpeed = (2f, 3f), freq = spectrum(6), panSpeed = (8f, 5f), bus = 32)

    sine(start, dur = dur, amp = (0.4f, 0.2f), ampSpeed = (8f, 13f), freq = spectrum(35), panSpeed = (3f, 2f), bus = 33)
    sine(start, dur = dur, amp = (0.3f, 0.3f), ampSpeed = (13f, 8f), freq = spectrum(38), panSpeed = (5f, 8f), bus = 34)
    sine(start, dur = dur, amp = (0.2f, 0.1f), ampSpeed = (5f, 21f), freq = spectrum(40), panSpeed = (13f, 8f), bus = 35)

    sine(start, dur = dur, amp = (0.04f, 0.02f), ampSpeed = (2f, 1f), freq = spectrum(80), panSpeed = (21f, 13f), bus = 36)
    sine(start, dur = dur, amp = (0.03f, 0.01f), ampSpeed = (1f, 2f), freq = spectrum(85), panSpeed = (13f, 34f), bus = 37)
    sine(start, dur = dur, amp = (0.02f, 0.01f), ampSpeed = (5f, 3f), freq = spectrum(90), panSpeed = (34f, 21f), bus = 38)
  }

  def straightSine(start: Float = 0f, dur: Float, freqs: (Float, Float), amp: Float, ampAttack: Float = 0.5f, panValue: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val sine = new SineInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freqs._1, freqs._2))
      .ampBus.control(ar(dur, ampAttack, (0f, amp, 0f)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(line(dur, panValue._1, panValue._2))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), sine ++ pan)
  }

  def straightPulse(start: Float = 0f, dur: Float, freqs: (Float, Float), amp: Float, ampAttack: Float = 0.5f, widthValue: (Float, Float) = (0.5f, 0.5f), panValue: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val pulse = new PulseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .freqBus.control(line(dur, freqs._1, freqs._2))
      .widthBus.control(line(dur, widthValue._1, widthValue._2))
      .ampBus.control(ar(dur, ampAttack, (0f, amp, 0f)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(line(dur, panValue._1, panValue._2))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), pulse ++ pan)
  }


  def straightWhite(start: Float, dur: Float, amp: Float, ampAttack: Float, panValue: (Float, Float), filter: InstrumentBuilder, bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val noise = new WhiteNoiseInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(ar(dur, ampAttack, (0f, amp, 0f)))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(line(dur, panValue._1, panValue._2))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), noise ++ filter.buildInstruments() ++ pan)
  }

  def straightWhiteHighpass(start: Float, dur: Float, amp: Float, ampAttack: Float, panValue: (Float, Float), filterFreq: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val highpass = new HighpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(line(dur, start = filterFreq._1, end = filterFreq._2))
    straightWhite(start, dur, amp, ampAttack, panValue, highpass, bus)

  }

  def straightWhiteLowpass(start: Float, dur: Float, amp: Float, ampAttack: Float, panValue: (Float, Float), filterFreq: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val lowpass = new LowpassReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .freqBus.control(line(dur, start = filterFreq._1, end = filterFreq._2))
    straightWhite(start, dur, amp, ampAttack, panValue, lowpass, bus)
  }

  def straightWhiteFilter(start: Float, dur: Float, amp: Float, ampAttack: Float, panValue: (Float, Float), filterFreq: (Float, Float), bws: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {

    val filter = new FilterReplaceInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .in(bus)
      .dur(dur)
      .ampBus.control(ar(dur, ampAttack, (0f, amp, 0f)))
      .bwBus.control(line(dur,bws._1, bws._2))
      .freqBus.control(line(dur, filterFreq._1, filterFreq._2))

    straightWhite(start, dur, amp, ampAttack, panValue, filter, bus)
  }

  def straightDust(start: Float = 0f, dur: Float, freqs: (Float, Float), amp: Float, ampAttack: Float = 0.5f, panValue: (Float, Float), bus: Int = 16)(implicit player: MusicPlayer): Unit = {
    val dust = new DustInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .out(bus)
      .dur(dur)
      .ampBus.control(ar(dur, ampAttack, (0f, amp, 0f)))
      .freqBus.control(line(dur, freqs._1, freqs._2))
      .buildInstruments()

    val pan = new PanInstrumentBuilder()
      .addAction(TAIL_ACTION)
      .dur(dur)
      .in(bus)
      .out(0)
      .panBus.control(line(dur, panValue._1, panValue._2))
      .buildInstruments()

    player.sendNew(absoluteTimeToMillis(start), dust ++ pan)
  }

  def prologuePulse(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = invertedSpectrum(0)
    println(s"Prologue, pulse. Duration: $dur")

    straightPulse(start = start, dur = invertedSpectrum(0), freqs = (spectrum(0), invertedSpectrum(20)), amp = 0.2f, ampAttack = 0.7f, panValue = (0f, 1f), bus = 42)
    straightPulse(start = start, dur = invertedSpectrum(0), freqs = (invertedSpectrum(6), invertedSpectrum(2)), amp = 0.1f, ampAttack = 0.3f, panValue = (0f, -1f), bus = 43)
    straightPulse(start = start, dur = invertedSpectrum(0), freqs = (spectrum(3), invertedSpectrum(13)), amp = 0.14f, ampAttack = 0.5f, panValue = (-0.5f, 0.5f), bus = 44)
    straightPulse(start = start, dur = invertedSpectrum(0), freqs = (spectrum(2), spectrum(5)), amp = 0.1f, ampAttack = 0.5f, panValue = (0.5f, -0.5f), bus = 45)
  }

  def transition1sine(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = invertedSpectrum(1)

    straightSine(start = start, dur = dur, freqs = (spectrum(20), spectrum(20)), amp = 0.2f, ampAttack = 0.3f, panValue = (0f, 1f), bus = 46)
    straightSine(start = start + invertedSpectrum(2), dur = dur, freqs = (spectrum(10), spectrum(10)), amp = 0.1f, ampAttack = 0.7f, panValue = (0f, -1f), bus = 47)
    straightSine(start = start + invertedSpectrum(1), dur = dur, freqs = (spectrum(25), spectrum(25)), amp = 0.14f, ampAttack = 0.5f, panValue = (-0.5f, 0.5f), bus = 48)
  }

  def transition2noise(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = invertedSpectrum(1)
    straightWhiteHighpass(start = start, dur = dur, amp = 0.5f, ampAttack = 0.5f, panValue = (-0.5f, 0.5f), filterFreq = (spectrum(60), spectrum(60)), bus = 49)
    straightWhiteLowpass(start = start + invertedSpectrum(2), dur = dur, amp = 0.9f, ampAttack = 0.7f, panValue = (0f, -1f),filterFreq = (spectrum(13), spectrum(13)), bus = 50)
    straightWhiteFilter(start = start + invertedSpectrum(1), dur = dur, amp = 0.3f, ampAttack = 0.3f, panValue = (0f, 1f), filterFreq =  (spectrum(20), spectrum(20)), bws = (0.01f, 0.01f), bus = 51)
  }


  def epilogueDust(start: Float = 0f)(implicit player: MusicPlayer): Unit = {
    val dur = invertedSpectrum(0)

    straightDust(start = start, dur = dur, freqs = (spectrum(2), invertedSpectrum(150)), 2.0f, 0.3f, panValue = (-0.1f, -0.5f), bus = 54)
    straightDust(start = start + invertedSpectrum(15), dur = dur, freqs = (spectrum(3), invertedSpectrum(170)), 2.0f, 0.5f, panValue = (0.11f, 0.5f), bus = 55)

    straightDust(start = start + invertedSpectrum(10), dur = dur, freqs = (spectrum(5), invertedSpectrum(100)), 2.0f, 0.5f, panValue = (-1f, 1f), bus = 52)
    straightDust(start = start + invertedSpectrum(8), dur = dur, freqs = (spectrum(7), invertedSpectrum(135)), 2.0f, 0.7f, panValue = (1f, -1f), bus = 53)
  }

  def main(args: Array[String]): Unit = {
    println(s"spectrum: $spectrum")
    println(s"Inverted spectrum: $invertedSpectrum")

    BusGenerator.reset()
    implicit val player: MusicPlayer = MusicPlayer()

    player.startPlay()

    setupNodes(player)
    //prologuePulse()
    //ambient1noise()
    //transition1sine()
    //ambient2pulse()
    //transition2noise()
    //ambient3sine()
    epilogueDust()

    Thread.sleep(5000)

  }
}
