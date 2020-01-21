package playground.tests

import utest._
import playground._
import freechips.rocketchip._
import config._
import wishbone._
import system._
import diplomacy._
import devices.tilelink._
import subsystem._
import arty100t._

object play extends TestSuite {
  val tests = Tests {
    test("play") {
      val config = new Config(new WithJtagDTM ++ new Config((site, here, up) => {
        case BootROMParams => new BootROMParams(contentFileName = "rocketchip/bootrom/bootrom.img")
      }) ++ new DefaultFPGAConfig)
      /**
       * generate the ExampleRocketSystem from config
       * notice this has been parameterized all Module
       **/
      val lm = LazyModule(playground.configToLazyModule(classOf[ExampleRocketSystem], config))
      val target = lm.getChildren.filter(_.name == "bh").head
    }
    test("wishbone") {
      val lm = LazyModule(playground.configToLazyModule(classOf[WishboneDemoMaster], new Config(Parameters.empty)))
      chisel3.Driver.emitVerilog(lm.module)
    }
    test("arty") {
      chisel3.Driver.emitVerilog(new FPGATop)
    }
    test("svd") {
      val lm = LazyModule(configToRocketModule(classOf[CustomArty100TRocketSystem], new CustomArty100TConfig))
      RocketModuleToSvd(lm)
    }
  }
}