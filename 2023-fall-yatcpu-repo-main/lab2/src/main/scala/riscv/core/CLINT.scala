// Copyright 2021 Howard Lau
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package riscv.core

import Chisel.Cat
import chisel3._
import chisel3.util.MuxLookup
import riscv.Parameters

object InterruptStatus {
  val None = 0x0.U(8.W)
  val Timer0 = 0x1.U(8.W)
  val Ret = 0xFF.U(8.W)
}

object InterruptEntry {
  val Timer0 = 0x4.U(8.W)
}

object InterruptState {
  val Idle = 0x0.U
  val SyncAssert = 0x1.U
  val AsyncAssert = 0x2.U
  val MRET = 0x3.U
}

object CSRState {
  val Idle = 0x0.U
  val Traping = 0x1.U
  val Mret = 0x2.U
}

class CSRDirectAccessBundle extends Bundle {
  val mstatus = Input(UInt(Parameters.DataWidth))
  val mepc = Input(UInt(Parameters.DataWidth))
  val mcause = Input(UInt(Parameters.DataWidth))
  val mtvec = Input(UInt(Parameters.DataWidth))

  val mstatus_write_data= Output(UInt(Parameters.DataWidth))
  val mepc_write_data= Output(UInt(Parameters.DataWidth))
  val mcause_write_data= Output(UInt(Parameters.DataWidth))

  val direct_write_enable = Output(Bool())
}

// Core Local Interrupt Controller
class CLINT extends Module {
  val io = IO(new Bundle {
    // Interrupt signals from peripherals
    val interrupt_flag = Input(UInt(Parameters.InterruptFlagWidth))

    val instruction = Input(UInt(Parameters.InstructionWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))

    val jump_flag = Input(Bool())
    val jump_address = Input(UInt(Parameters.AddrWidth))

    val interrupt_handler_address = Output(UInt(Parameters.AddrWidth))
    val interrupt_assert = Output(Bool())

    val csr_bundle = new CSRDirectAccessBundle
  })
  val interrupt_enable = io.csr_bundle.mstatus(3)
  val instruction_address = Mux(
    io.jump_flag,
    io.jump_address,
    io.instruction_address + 4.U,
  )
  //lab2(CLINTCSR)
  //mstatus寄存器中MIE位是第3位 MPIE是第7位
  //0x80000007L Machine Timer Interrupt 表示由timer造成的外部中断
  //0x8000000BL Machine External Interrupt 表示由uart造成的软件中断
  //interrupt_flag第0位来确定程序中断是外部中断还是软件中断
  //需导入Cat 否则试用'#'来链接字段
  when(io.interrupt_flag =/= InterruptStatus.None && interrupt_enable) {
    io.csr_bundle.mstatus_write_data := Cat(io.csr_bundle.mstatus(31, 4), 0.U(1.W), io.csr_bundle.mstatus(2, 0))
    io.csr_bundle.mepc_write_data := instruction_address
    io.csr_bundle.mcause_write_data := Mux(io.interrupt_flag(0), 0x80000007L.U, 0x8000000BL.U)
    io.csr_bundle.direct_write_enable := true.B
    io.interrupt_assert := true.B
    io.interrupt_handler_address := io.csr_bundle.mtvec
  }.elsewhen(io.instruction === InstructionsRet.mret) {
    //MIE位为MPIE的值
    //返回原先被中断的程序地址 利用中断的形式中断中断程序
    io.csr_bundle.mstatus_write_data := Cat(io.csr_bundle.mstatus(31, 4), io.csr_bundle.mstatus(7), io.csr_bundle.mstatus(2, 0))
    io.csr_bundle.mepc_write_data := io.csr_bundle.mepc //保持原样
    io.csr_bundle.mcause_write_data := io.csr_bundle.mcause //保持原样
    io.csr_bundle.direct_write_enable := true.B
    io.interrupt_assert := true.B
    io.interrupt_handler_address := io.csr_bundle.mepc //回到之前执行的位置
  }.otherwise {
    io.csr_bundle.mstatus_write_data := Cat(io.csr_bundle.mstatus(31, 4), 0.U(1.W), io.csr_bundle.mstatus(2, 0))
    io.csr_bundle.mepc_write_data := instruction_address
    io.csr_bundle.mcause_write_data := Mux(io.interrupt_flag(0), 0x80000007L.U, 0x8000000BL.U)
    io.csr_bundle.direct_write_enable := false.B
    io.interrupt_assert := false.B
    io.interrupt_handler_address := io.csr_bundle.mtvec
  }
}
