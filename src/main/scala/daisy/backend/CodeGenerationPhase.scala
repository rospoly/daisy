// Copyright 2017 MPI-SWS, Saarbruecken, Germany

package daisy
package backend

import daisy.utils.CodePrinter

import lang.Trees._
import tools.FinitePrecision._
import lang.Types._
import lang.Extractors.ArithOperator
import tools.{Interval, Rational}
import lang.Identifiers.Identifier

object CodeGenerationPhase extends DaisyPhase {
  override val name = "Code Generation"
  override val shortName = "codegen"
  override val description = "Generates (executable) code."
  override val definedOptions: Set[CmdLineOption[Any]] = Set(
    StringChoiceOption(
      "lang",
      Set("C", "Scala", "FPCore"),
      "Scala",
      "Language for which to generate code"),
    FlagOption(
      "genMain",
      "Whether to generate a main method to run the code.")
  )

  implicit val debugSection = DebugSectionBackend

  var reporter: Reporter = null

  def runPhase(ctx: Context, prg: Program): (Context, Program) = {
    val mixedPrecision = ctx.option[Option[String]]("mixed-precision").isDefined
    val uniformPrecision = ctx.option[Precision]("precision")
    val lang = ctx.option[String]("lang")

    reporter = ctx.reporter

    val newProgram = uniformPrecision match {
      case FixedPrecision(b) =>
        if (mixedPrecision) {
          ctx.reporter.error("Mixed-precision code generation is currently not supported for fixed-points.")
        }
        // if we have fixed-point code, we need to generate it first
        // TODO handle ignored functions
        val newDefs = transformConsideredFunctions(ctx,prg){ fnc =>
          val newBody = fnc.body.map(toFixedPointCode(_, FixedPrecision(b),
            ctx.intermediateRanges(fnc.id), ctx.intermediateAbsErrors(fnc.id)))
          val valDefType = b match {
            case 8 => Int16Type
            case 16 => Int32Type
            case 32 => Int64Type
          }
          fnc.copy(
            params = fnc.params.map(vd => ValDef(vd.id.changeType(valDefType))),
            body = newBody,
            returnType = valDefType)
        }
        Program(prg.id, newDefs)
      case up @ FloatPrecision(_) =>
        // if we have floating-point code, we need to just change the types
        Program(prg.id, prg.defs.map { fnc =>
          assignFloatType(fnc, ctx.specInputPrecisions(fnc.id), ctx.specResultPrecisions(fnc.id), up)
        })
    }

    writeFile(newProgram, lang, ctx)

    (ctx, newProgram)
  }

  private def writeFile(prg: Program, lang: String, ctx: Context): Unit = {
    import java.io.FileWriter
    import java.io.BufferedWriter
    val filename = System.getProperty("user.dir")+"/output/" + prg.id + CodePrinter.suffix(lang)
    ctx.codegenOutput.append(prg.id)
    val fstream = new FileWriter(filename)
    val out = new BufferedWriter(fstream)
    CodePrinter(prg, ctx, lang, out)
  }

  private def assignFloatType(fnc: FunDef, typeMap: Map[Identifier, Precision],
                              returnType: Precision, defaultPrecision: Precision): FunDef = {

    def changeType(e: Expr, tpeMap: Map[Identifier, Precision]): (Expr, Precision) = e match {

      case Variable(id) =>
        (Variable(id.changeType(FinitePrecisionType(tpeMap(id)))), tpeMap(id))

      case x @ RealLiteral(r) =>
        (FinitePrecisionLiteral(r, defaultPrecision, x.stringValue), defaultPrecision)

      case ArithOperator(es_old, recons) =>
        val (es, ps) = es_old.unzip(changeType(_, tpeMap))

        val prec = getUpperBound(ps: _*)
        (recons(es), prec)

      case Let(id, value, body) =>
        val (eValue, valuePrec) = changeType(value, tpeMap)
        val (eBody, bodyPrec) = changeType(body, tpeMap)

        val idPrec = tpeMap(id)

        if (idPrec >= valuePrec) {
          (Let(id.changeType(FinitePrecisionType(tpeMap(id))), eValue, eBody), bodyPrec)
        } else {
          val newValue = Downcast(eValue, FinitePrecisionType(idPrec))
          (Let(id.changeType(FinitePrecisionType(tpeMap(id))), newValue, eBody), bodyPrec)
        }
    }

    fnc.copy(
      returnType = FinitePrecisionType(returnType),
      params = fnc.params.map(vd => ValDef(vd.id.changeType(FinitePrecisionType(typeMap(vd.id))))),
      // this should really be changed too
      body = fnc.body.map(changeType(_, typeMap)._1)
    )
  }

  /*
   * Expects code to be already in SSA form.
   * @param fixed the (uniform) fixed-point precision to use
   * TODO: we also need to adjust the types, no?
   */
  def toFixedPointCode(expr: Expr, format: FixedPrecision, intermRanges: Map[Expr, Interval],
                       intermAbsErrors: Map[Expr, Rational]): Expr = {
    val newType = format match {
      case FixedPrecision(8) => Int16Type
      case FixedPrecision(16) => Int32Type
      case FixedPrecision(32) => Int64Type
    }

    @inline
    def getFractionalBits(e: Expr): Int = {
      // the overall interval is the real-valued range +/- absolute errors
      val interval = intermRanges(e) +/- intermAbsErrors(e)
      format.fractionalBits(interval)
    }


    def _toFPCode(e: Expr): Expr = (e: @unchecked) match {
      case x @ Variable(id) => Variable(id.changeType(newType))

      case RealLiteral(r) => // TODO: translate constant
        val f = format.fractionalBits(r)
        format match {
          case FixedPrecision(8) => Int16Literal((r * Rational.fromDouble(math.pow(2, f))).roundToInt)
          case FixedPrecision(16) => Int32Literal((r * Rational.fromDouble(math.pow(2, f))).roundToInt)
          case FixedPrecision(32) => Int64Literal((r * Rational.fromDouble(math.pow(2, f))).roundToLong)
        }

      case UMinus(t) => UMinus(_toFPCode(t))

      case Sqrt(t) =>
        throw new Exception("Sqrt is not supported for fixed-points!")

      case x @ Plus(lhs, rhs) =>
        val fLhs = getFractionalBits(lhs)
        val fRhs = getFractionalBits(rhs)

        // determine how much to shift left or right
        val fAligned = math.max(fLhs, fRhs)
        val newLhs =
          if (fLhs < fAligned) {
            LeftShift(_toFPCode(lhs), (fAligned - fLhs))
          } else {
            _toFPCode(lhs)
          }
        val newRhs =
          if (fRhs < fAligned) {
            LeftShift(_toFPCode(rhs), (fAligned - fRhs))
          } else {
            _toFPCode(rhs)
          }

        // fractional bits result
        val fRes = getFractionalBits(x)
        // shift result
        if (fAligned == fRes) {
          Plus(newLhs, newRhs)
        } else if (fRes < fAligned) {
          RightShift(Plus(newLhs, newRhs), (fAligned - fRes))
        } else { // (fAligned < fRes) {
          // TODO: this sounds funny. does this ever happen?
          //reporter.warning("funny shifting condition is happening")
          LeftShift(Plus(newLhs, newRhs), (fRes - fAligned))

        }

      case x @ Minus(lhs, rhs) =>
        // fractional bits from lhs
        val fLhs = getFractionalBits(lhs)
        val fRhs = getFractionalBits(rhs)

        // determine how much to shift left or right
        val fAligned = math.max(fLhs, fRhs)
        val newLhs =
          if (fLhs < fAligned) {
            LeftShift(_toFPCode(lhs), (fAligned - fLhs))
          } else {
            _toFPCode(lhs)
          }
        val newRhs =
          if (fRhs < fAligned) {
            LeftShift(_toFPCode(rhs), (fAligned - fRhs))
          } else {
            _toFPCode(rhs)
          }

        // fractional bits result
        val fRes = getFractionalBits(x)
        // shift result
        if (fAligned == fRes) {
          Minus(newLhs, newRhs)
        } else if (fRes < fAligned) {
          RightShift(Minus(newLhs, newRhs), (fAligned - fRes))
        } else { // (fAligned < fRes) {
          // TODO: this sounds funny. does this ever happen?
          //reporter.warning("funny shifting condition is happening")
          LeftShift(Minus(newLhs, newRhs), (fRes - fAligned))
        }

      case x @ Times(lhs, rhs) =>

        val mult = Times(_toFPCode(lhs), _toFPCode(rhs))
        val fMult = getFractionalBits(lhs) + getFractionalBits(rhs)

        // fractional bits result
        val fRes = getFractionalBits(x)
        // shift result
        if (fMult == fRes) {
          mult
        } else if (fRes < fMult) {
          RightShift(mult, (fMult - fRes))
        } else { // (fAligned < fRes) {
          // TODO: this sounds funny. does this ever happen?
          //reporter.warning("funny shifting condition is happening")
          LeftShift(mult, (fRes - fMult))
        }

      case x @ Division(lhs, rhs) =>
        val fLhs = getFractionalBits(lhs)
        val fRhs = getFractionalBits(rhs)

        val fRes = getFractionalBits(x)
        val shift = fRes + fRhs - fLhs
        Division(LeftShift(_toFPCode(lhs), shift), _toFPCode(rhs))

      case Let(id, value, body) =>
        Let(id.changeType(newType), _toFPCode(value), _toFPCode(body))
    }

    _toFPCode(expr)
  }
}
