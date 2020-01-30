package wishbone

import chisel3._
import chisel3.experimental.DataMirror
import freechips.rocketchip.util.GenericParameterizedBundle

abstract class WBBundleBase(params: WBBundleParameters) extends GenericParameterizedBundle(params)

// Signal directions are from the master's point-of-view
class WBBundle(params: WBBundleParameters) extends WBBundleBase(params) {

  /**
   * dual-way signals
   **/
  /**
   * The data output array [DAT_O()] is used to pass binary data.
   * The array boundaries are determined by the port size, with a maximum port size of 64-bits (e.g. [DAT_I(63..0)]).
   * Also see the [DAT_I()] and [SEL_O()] signal descriptions.
   **/
  val dataOut = Output(UInt(params.dataBits.W))
  /**
   * The data input array [DAT_I()] is used to pass binary data.
   * The array boundaries are determined by the port size, with a maximum port size of 64-bits (e.g. [DAT_I(63..0)]).
   * Also see the [DAT_O()] and [SEL_O()] signal descriptions.
   **/
  val dataIn = Input(UInt(params.dataBits.W))
  /**
   * Data tag type [TGD_I()] is used on MASTER and SLAVE interfaces.
   * It contains information that is associated with the data input array [DAT_I()], and is qualified by signal [STB_I].
   * For example, parity protection, error correction and time stamp information can be attached to the data bus.
   * These tag bits simplify the task of defining new signals because their timing(in relation to every bus cycle) is pre-defined by this specification.
   * The name and operation of a data tag must be defined in the WISHBONE DATASHEET.
   **/
  val dataTagOut = Output(UInt(params.dataTagBits.W))
  /**
   * Data tag type [TGD_O()] is used on MASTER and SLAVE interfaces.
   * It contains information that is associated with the data output array [DAT_O()], and is qualified by signal [STB_O].
   * For example, parity protection, error correction and time stamp information can be attached to the data bus.
   * These tag bits simplify the task of defining new signals because their timing (in relation to every bus cycle) is pre-defined by this specification.
   * The name and operation of a data tag must be defined in the WISHBONE DATASHEET.
   **/
  val dataTagIn = Input(UInt(params.dataTagBits.W))

  /**
   * one-way signals
   **/
  /**
   * The acknowledge input [ACK_I], when asserted, indicates the normal termination of a bus cycle.
   * Also see the [ERR_I] and [RTY_I] signal descriptions.
   **/
  val acknowledge = Input(Bool())
  /**
   * The address output array [ADR_O()] is used to pass a binary address.
   * The higher array boundary is specific to the address width of the core,
   * and the lower array boundary is determined by the data port size and granularity.
   * For example the array size on a 32-bit data port with BYTE granularity is [ADR_O(n..2)].
   * In some cases (such as FIFO interfaces) the array may not be present on the interface.
   **/
  val address = Output(UInt(params.addrBits.W))
  /**
   * The cycle output [CYC_O], when asserted, indicates that a valid bus cycle is in progress.
   * The signal is asserted for the duration of all bus cycles.
   * For example, during a BLOCK transfer cycle there can be multiple data transfers.
   * The [CYC_O] signal is asserted during the first data transfer, and remains asserted until the last data transfer.
   * The [CYC_O] signal is useful for interfaces with multi-port interfaces (such as dual port memories).
   * In these cases, the [CYC_O] signal requests use of a common bus from an arbiter.
   **/
  val cycle = Output(Bool())
  /**
   * The pipeline stall input [STALL_I] indicates that current slave is not able to accept the transfer in the transaction queue.
   * This signal is used in pipelined mode.
   **/
  val stall = Input(Bool())
  /**
   * The error input [ERR_I] indicates an abnormal cycle termination.
   * The source of the error, and the response generated by the MASTER is defined by the IP core supplier.
   * Also see the [ACK_I] and [RTY_I] signal descriptions.
   **/
  val error = Input(Bool())
  /**
   * The lock output [LOCK_O] when asserted, indicates that the current bus cycle is uninterruptible.
   * Lock is asserted to request complete ownership of the bus.
   * Once the transfer has started, the INTERCON does not grant the bus to any other MASTER, until the current MASTER negates [LOCK_O] or [CYC_O]. */
  val lock = Output(Bool())
  /**
   * The retry input [RTY_I] indicates that the interface is not ready to accept or send data, and that the cycle should be retried.
   * When and how the cycle is retried is defined by the IP core supplier.
   * Also see the [ERR_I] and [RTY_I] signal descriptions.
   **/
  val retry = Input(Bool())
  /**
   * The select output array [SEL_O()] indicates where valid data is expected on the [DAT_I()] signal array during READ cycles,
   * and where it is placed on the [DAT_O()] signal array during WRITE cycles.
   * The array boundaries are determined by the granularity of a port.
   * For example, if 8-bit granularity is used on a 64-bit port, then there would be an array of eight select signals with boundaries of [SEL_O(7..0)].
   * Each individual select signal correlates to one of eight active bytes on the 64-bit data port.
   * For more information about Wishbone B4[SEL_O()], please refer to the data organization section in Chapter 3 of this specification.
   * Also see the [DAT_I()], [DAT_O()] and [STB_O] signal descriptions.
   **/
  val select = Output(UInt(params.selectBits.W))
  /**
   * The strobe output [STB_O] indicates a valid data transfer cycle.
   * It is used to qualify various other signals on the interface such as [SEL_O()].
   * The SLAVE asserts either the[ACK_I], [ERR_I] or [RTY_I] signals in response to every assertion of the [STB_O] signal.
   **/
  val strobe = Output(Bool())
  /**
   * Address tag type [TGA_O()] contains information associated with address lines [ADR_O()], and is qualified by signal [STB_O].
   * For example, address size (24-bit, 32-bit etc.) and memory management (protected vs. unprotected) information can be attached to an address.
   * These tag bits simplify the task of defining new signals because their timing(in relation to every bus cycle) is defined by this specification.
   * The name and operation ofan address tag must be defined in the WISHBONE DATASHEET.
   **/
  val addressTag = Output(UInt(params.addressTagBits.W))
  /** Cycle tag type [TGC_O()] contains information associated with bus cycles, and is qualified by signal [CYC_O].
   * For example, data transfer, interrupt acknowledge and cache control cycles can be uniquely identified with the cycle tag.
   * They can also be used to discriminate between WISHBONE SINGLE, BLOCK and RMW cycles.
   * These tag bits simplify the task of defining new signals because their timing (in relation to every bus cycle) is defined by this specification.
   * The name and operation of a cycle tag must be defined in the WISHBONE DATASHEET. */
  val cycleTag = Output(UInt(params.clockTagBits.W))
  /** The write enable output [WE_O] indicates whether the current local bus cycle is a READ or WRITE cycle.
   * The signal is negated during READ cycles, and is asserted during WRITE cycles. */
  val writeEnable = Output(Bool())

  def tieoff(): Unit = DataMirror.directionOf(stall) match {
    case ActualDirection.Input =>
      stall := true.B
      acknowledge := false.B
      dataIn := false.B
      dataTagIn := false.B
      retry := false.B
    case ActualDirection.Output =>
      dataOut := false.B
      dataTagOut := false.B
      addressTag := false.B
      address := false.B
      cycle := false.B
      lock := false.B
      strobe := false.B
      writeEnable := false.B
    case _ =>
  }
}

object WBBundle {
  def apply(params: WBBundleParameters) = new WBBundle(params)
}
