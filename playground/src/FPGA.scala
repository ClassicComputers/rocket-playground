package arty100t

import chisel3._
import freechips.rocketchip._
import amba.axi4.AXI4Bundle
import chisel3.experimental._
import diplomacy._
import devices.debug.SystemJTAGIO
import sifive.blocks.devices._
import uart._
import spi._
import playground._
import sifive.fpgashells.ip.xilinx._


class FPGATop extends MultiIOModule {
  val top = Module(LazyModule(configToRocketModule(classOf[CustomArty100TRocketSystem], new CustomArty100TConfig)).module)

  val topInterrupts: UInt = top.interrupts
  val fpgaInterrupts = IO(Input(topInterrupts.cloneType))
  fpgaInterrupts <> topInterrupts

  val topMem: AXI4Bundle = top.outer.mem_axi4.head
  val fpgaMem = IO(topMem.cloneType)
  fpgaMem <> topMem

  val topUART: UARTPortIO = top.uart.head.asInstanceOf[UARTPortIO]
  val fpgaUART = IO(new Bundle {
    val txd = Analog(1.W)
    val rxd = Analog(1.W)
  })
  IOBUF(fpgaUART.rxd, topUART.txd)
  topUART.rxd := IOBUF(fpgaUART.txd)

  val topSPI: SPIPortIO = top.qspi.head.asInstanceOf[SPIPortIO]
  val fpgaSPI = IO(new Bundle {
    val sck = Analog(1.W)
    val cs = Analog(1.W)
    val dq = Vec(4, Analog(1.W))
  })

  IOBUF(fpgaSPI.sck, topSPI.sck)
  IOBUF(fpgaSPI.cs, topSPI.cs(0))
  fpgaSPI.dq.zipWithIndex.foreach {
    case (io: Analog, i: Int) =>
      val pad = Module(new IOBUF)
      pad.io.I := topSPI.dq(i).o
      topSPI.dq(i).i := pad.io.O
      pad.io.T := ~topSPI.dq(i).oe
      attach(pad.io.IO, io)
      PULLUP(io)
  }


  val topJtag: SystemJTAGIO = top.debug.head.systemjtag.head
  val fpgaJtag = IO(new Bundle {
    val tck = Analog(1.W)
    val tms = Analog(1.W)
    val tdi = Analog(1.W)
    val tdo = Analog(1.W)
  })
  topJtag.reset := reset
  topJtag.mfr_id := 0x489.U(11.W)
  topJtag.part_number := 0.U(16.W)
  topJtag.version := 2.U(4.W)
  topJtag.jtag.TCK := IBUFG(IOBUF(fpgaJtag.tck).asClock)
  topJtag.jtag.TMS := IOBUF(fpgaJtag.tms)
  PULLUP(fpgaJtag.tms)
  topJtag.jtag.TDI := IOBUF(fpgaJtag.tdi)
  PULLUP(fpgaJtag.tdi)
  IOBUF(fpgaJtag.tdo, topJtag.jtag.TDO.data)
  PULLUP(fpgaJtag.tdo)

  /** second QSPI will become SDIO */
  val topSDIO: SPIPortIO = top.qspi.last.asInstanceOf[SPIPortIO]

  val fpgaSDIO = IO(new Bundle {
    val cmd = Analog(1.W)
    val sck = Analog(1.W)
    val dat = Vec(4, Analog(1.W))
  })

  val misoSync = RegInit(VecInit(Seq.fill(2)(false.B)))
  val miso = Wire(Bool())
  val mosi = Wire(Bool())
  mosi := topSDIO.dq(0).o
  misoSync(0) := miso
  misoSync(1) := misoSync(0)
  topSDIO.dq(0).i := false.B
  topSDIO.dq(1).i := false.B
  topSDIO.dq(2).i := misoSync(1)
  topSDIO.dq(3).i := false.B
  IOBUF(fpgaSDIO.sck, topSDIO.sck)
  IOBUF(fpgaSDIO.cmd, mosi)
  miso := IOBUF(fpgaSDIO.dat(0))
  IOBUF(fpgaSDIO.dat(3), topSDIO.cs(0))
}