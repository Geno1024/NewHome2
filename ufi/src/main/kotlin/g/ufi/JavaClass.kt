@file:OptIn(ExperimentalUnsignedTypes::class)

package g.ufi

import g.ufi.utils.read1
import g.ufi.utils.read2UBE
import g.ufi.utils.read4UBE
import g.ufi.utils.toUByteArrayBE
import java.io.InputStream
import java.io.OutputStream

data class JavaClass(
    var magic: UInt = 0xCAFEBABE.toUInt(),
    var minorVersion: UShort = 0U,
    var majorVersion: MajorVersion = MajorVersion.JAVA_1_8,
    var constantPoolCount: UShort = 0U,
    var constantPool: MutableList<Constant> = mutableListOf(),
    var accessFlags: MutableList<ClassAccessFlag> = mutableListOf(),
    var thisClass: UShort = 0U,
    var superClass: UShort = 0U,
    var interfacesCount: UShort = 0U,
    var interfaces: MutableList<Interface> = mutableListOf(),
    var fieldsCount: UShort = 0U,
    var fields: MutableList<Field> = mutableListOf(),
    var methodsCount: UShort = 0U,
    var methods: MutableList<Method> = mutableListOf(),
    var attributesCount: UShort = 0U,
    var attributes: MutableList<Attribute> = mutableListOf()
): UFI
{
    companion object
    {
        fun read(input: InputStream): JavaClass = with (input)
        {
            JavaClass(
                read4UBE(),
                read2UBE(),
                MajorVersion.fromValue(read2UBE())
            ).apply {
                constantPoolCount = read2UBE()
                constantPool = mutableListOf(Constant())
                (1..constantPoolCount.toInt()).forEach { index ->
                    if (constantPool.size == index) return@forEach
                    val c = Constant.read(input)
                    constantPool.add(c)
                    if ((c is Constant.Long) or (c is Constant.Double))
                    {
                        constantPool.add(c)
                    }
                }
                accessFlags = ClassAccessFlag.fromValue(read2UBE()).toMutableList()
                thisClass = read2UBE()
                superClass = read2UBE()
                interfacesCount = read2UBE()
                repeat(interfacesCount.toInt()) {
                    interfaces.add(Interface(read2UBE()))
                }
                fieldsCount = read2UBE()
                repeat(fieldsCount.toInt()) {
                    fields.add(Field.read(input, constantPool))
                }
                methodsCount = read2UBE()
                repeat(methodsCount.toInt()) {
                    methods.add(
                        Method(
                            MethodAccessFlag.fromValue(read2UBE()).toMutableList(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        ).apply method@ {
                            repeat(this@method.attributesCount.toInt()) {
                                attributes.add(Attribute.read(input, constantPool))
                            }
                        }
                    )
                }
                attributesCount = read2UBE()
                repeat(attributesCount.toInt()) {
                    attributes.add(
                        Attribute.read(input, constantPool)
                    )
                }
            }
        }
    }

    override fun write(output: OutputStream)
    {
        TODO("Not yet implemented")
    }

    override fun read(input: InputStream) = JavaClass.read(input)

    enum class MajorVersion(val major: UShort)
    {
        JAVA_1_0(44U),
        JAVA_1_1(45U),
        JAVA_1_2(46U),
        JAVA_1_3(47U),
        JAVA_1_4(48U),
        JAVA_1_5(49U),
        JAVA_1_6(50U),
        JAVA_1_7(51U),
        JAVA_1_8(52U),
        JAVA_9(53U),
        JAVA_10(54U),
        JAVA_11(55U),
        JAVA_12(56U),
        JAVA_13(57U),
        JAVA_14(58U),
        JAVA_15(59U),
        JAVA_16(60U),
        JAVA_17(61U),
        JAVA_18(62U),
        JAVA_19(63U),
        JAVA_20(64U),
        JAVA_21(65U),
        JAVA_22(66U),
        JAVA_23(67U),
        JAVA_24(68U);

        companion object
        {
            fun fromValue(value: UShort) = entries.firstOrNull { it.major == value }?:JAVA_1_8
        }
    }
    
    open class Constant(
        var tag: Tag = Tag.Utf8,
        var info: UByteArray = UByteArray(0)
    )
    {
        companion object
        {
            fun read(input: InputStream): Constant = with (input) {
                val tag = Tag.fromValue(input.read1())
                when (tag)
                {
                    Tag.Utf8 -> Utf8(read2UBE(), this)
                    Tag.Integer -> Integer(read4UBE())
                    Tag.Float -> Float(read4UBE())
                    Tag.Long -> Long(read4UBE(), read4UBE())
                    Tag.Double -> Double(read4UBE(), read4UBE())
                    Tag.Class -> Class(read2UBE())
                    Tag.String -> String(read2UBE())
                    Tag.FieldRef -> FieldRef(read2UBE(), read2UBE())
                    Tag.MethodRef -> MethodRef(read2UBE(), read2UBE())
                    Tag.InterfaceMethodRef -> InterfaceMethodRef(read2UBE(), read2UBE())
                    Tag.NameAndType -> NameAndType(read2UBE(), read2UBE())
                    Tag.MethodHandle -> MethodHandle(read1(), read2UBE())
                    Tag.MethodType -> MethodType(read2UBE())
                    Tag.InvokeDynamic -> InvokeDynamic(read2UBE(), read2UBE())
                    Tag.Dynamic -> Dynamic(read2UBE(), read2UBE())
                    Tag.Module -> Module(read2UBE())
                    Tag.Package -> Package(read2UBE())
                }
            }
        }

        enum class Tag(val value: UByte)
        {
            Utf8(1U),

            Integer(3U),
            Float(4U),
            Long(5U),
            Double(6U),
            Class(7U),
            String(8U),
            FieldRef(9U),
            MethodRef(10U),
            InterfaceMethodRef(11U),
            NameAndType(12U),


            MethodHandle(15U),
            MethodType(16U),
            Dynamic(17U),
            InvokeDynamic(18U),
            Module(19U),
            Package(20U);

            companion object
            {
                fun fromValue(value: UByte) = entries.firstOrNull { it.value == value }?:Utf8
            }
        }

        data class Class(
            var nameIndex: UShort = 0U
        ) : Constant(Tag.Class, nameIndex.toUByteArrayBE())

        data class FieldRef(
            var classIndex: UShort = 0U,
            var nameAndTypeIndex: UShort = 0U
        ) : Constant(Tag.FieldRef, classIndex.toUByteArrayBE() + nameAndTypeIndex.toUByteArrayBE())

        data class MethodRef(
            var classIndex: UShort = 0U,
            var nameAndTypeIndex: UShort = 0U
        ) : Constant(Tag.MethodRef, classIndex.toUByteArrayBE() + nameAndTypeIndex.toUByteArrayBE())

        data class InterfaceMethodRef(
            var classIndex: UShort = 0U,
            var nameAndTypeIndex: UShort = 0U
        ) : Constant(Tag.InterfaceMethodRef, classIndex.toUByteArrayBE() + nameAndTypeIndex.toUByteArrayBE())

        data class String(
            var stringIndex: UShort = 0U
        ) : Constant(Tag.String, stringIndex.toUByteArrayBE())

        data class Integer(
            var bytes: UInt = 0U
        ) : Constant(Tag.Integer, bytes.toUByteArrayBE())

        data class Float(
            var bytes: UInt = 0U
        ) : Constant(Tag.Float, bytes.toUByteArrayBE())

        data class Long(
            var highUBytes: UInt = 0U,
            var lowUBytes: UInt = 0U
        ) : Constant(Tag.Long, highUBytes.toUByteArrayBE() + lowUBytes.toUByteArrayBE())

        data class Double(
            var highUBytes: UInt,
            var lowUBytes: UInt
        ) : Constant(Tag.Double, highUBytes.toUByteArrayBE() + lowUBytes.toUByteArrayBE())

        data class NameAndType(
            var nameIndex: UShort = 0U,
            var descriptorIndex: UShort = 0U
        ) : Constant(Tag.NameAndType, nameIndex.toUByteArrayBE() + descriptorIndex.toUByteArrayBE())

        data class Utf8(
            var length: UShort = 0U,
            var bytes: UByteArray = ubyteArrayOf()
        ) : Constant(Tag.Utf8, length.toUByteArrayBE() + bytes)
        {
            constructor(length: UShort, input: InputStream) : this(length, input.readNBytes(length.toInt()).toUByteArray())

            override fun toString(): kotlin.String = bytes.toByteArray().toString(Charsets.UTF_8)
        }

        data class MethodHandle(
            var referenceKind: UByte = 0U,
            var referenceIndex: UShort = 0U
        ) : Constant(Tag.MethodHandle, ubyteArrayOf(referenceKind) + referenceIndex.toUByteArrayBE())

        data class MethodType(
            var descriptorIndex: UShort = 0U
        ) : Constant(Tag.MethodHandle, descriptorIndex.toUByteArrayBE())

        data class InvokeDynamic(
            var bootstrapMethodAttrIndex: UShort = 0U,
            var nameAndTypeIndex: UShort = 0U
        ) : Constant(Tag.InvokeDynamic, bootstrapMethodAttrIndex.toUByteArrayBE() + nameAndTypeIndex.toUByteArrayBE())

        data class Dynamic(
            var bootstrapMethodAttrIndex: UShort = 0U,
            var nameAndTypeIndex: UShort = 0U
        ) : Constant(Tag.Dynamic, bootstrapMethodAttrIndex.toUByteArrayBE() + nameAndTypeIndex.toUByteArrayBE())

        data class Module(
            var nameIndex: UShort = 0U
        ) : Constant(Tag.Module, nameIndex.toUByteArrayBE())

        data class Package(
            var nameIndex: UShort = 0U
        ) : Constant(Tag.Module, nameIndex.toUByteArrayBE())
    }

    enum class ClassAccessFlag(val value: UShort)
    {
        PUBLIC(0x0001U),
        FINAL(0x0010U),
        SUPER(0x0020U),
        INTERFACE(0x0200U),
        ABSTRACT(0x0400U),
        SYNTHETIC(0x1000U),
        ANNOTATION(0x2000U),
        ENUM(0x4000U),
        MODULE(0x8000U);

        companion object
        {
            fun fromValue(value: UShort) = entries.filter { value and it.value == it.value }
        }
    }

    data class Interface(
        var index: UShort
    )

    data class Field(
        var accessFlags: MutableList<FieldAccessFlag> = mutableListOf(),
        var nameIndex: UShort = 0U,
        var descriptorIndex: UShort = 0U,
        var attributesCount: UShort = 0U,
        var attributes: MutableList<Attribute> = mutableListOf()
    )
    {
        companion object
        {
            fun read(input: InputStream, constantPool: MutableList<Constant>) = with(input) {
                Field(
                    FieldAccessFlag.fromValue(read2UBE()).toMutableList(),
                    read2UBE(),
                    read2UBE(),
                    read2UBE(),
                ).apply {
                    repeat(attributesCount.toInt()) {
                        attributes.add(Attribute.read(input, constantPool))
                    }
                }
            }
        }
    }

    enum class FieldAccessFlag(val value: UShort)
    {
        PUBLIC(0x0001U),
        PRIVATE(0x0002U),
        PROTECTED(0x0004U),
        STATIC(0x0008U),
        FINAL(0x0010U),
        VOLATILE(0x0040U),
        TRANSIENT(0x0080U),
        SYNTHETIC(0x1000U),
        ENUM(0x4000U);

        companion object
        {
            fun fromValue(value: UShort) = entries.filter { value and it.value == it.value }
        }
    }

    data class Method(
        var accessFlags: MutableList<MethodAccessFlag> = mutableListOf(),
        var nameIndex: UShort = 0U,
        var descriptorIndex: UShort = 0U,
        var attributesCount: UShort = 0U,
        var attributes: MutableList<Attribute> = mutableListOf()
    )

    enum class MethodAccessFlag(val value: UShort)
    {
        PUBLIC(0x0001U),
        PRIVATE(0x0002U),
        PROTECTED(0x0004U),
        STATIC(0x0008U),
        FINAL(0x0010U),
        SYNCHRONIZED(0x0020U),
        BRIDGE(0x0040U),
        VARARGS(0x0080U),
        NATIVE(0x0100U),
        ABSTRACT(0x0400U),
        STRICT(0x0800U),
        SYNTHETIC(0x1000U);

        companion object
        {
            fun fromValue(value: UShort) = entries.filter { value and it.value == it.value }
        }
    }

    open class Attribute(
        open var attributeNameIndex: UShort = 0U,
        open var attributeLength: UInt = 0U,
        var info: UByteArray = ubyteArrayOf()
    )
    {
        companion object
        {
            fun read(input: InputStream, constantPool: MutableList<Constant>) = with (input) {
                val attributeNameIndex = read2UBE()
                val attributeLength = read4UBE()
                when ((constantPool[attributeNameIndex.toInt()] as Constant.Utf8).toString())
                {
                    "ConstantValue" -> ConstantValue(attributeNameIndex, attributeLength, read2UBE())
                    "Code" -> Code(attributeNameIndex, attributeLength, input, constantPool)
                    "StackMapTable" -> StackMapTable(attributeNameIndex, attributeLength, input)
                    "Exceptions" -> Exceptions(attributeNameIndex, attributeLength, read2UBE(), input)
                    "InnerClasses" -> InnerClasses(attributeNameIndex, attributeLength, read2UBE(), input)
                    "EnclosingMethod" -> EnclosingMethod(attributeNameIndex, attributeLength, read2UBE(), read2UBE())
                    "Synthetic" -> Synthetic(attributeNameIndex, attributeLength)
                    "Signature" -> Signature(attributeNameIndex, attributeLength, read2UBE())
                    "SourceFile" -> SourceFile(attributeNameIndex, attributeLength, read2UBE())
                    "SourceDebugExtension" -> SourceDebugExtension(attributeNameIndex, attributeLength, readNBytes(attributeLength.toInt()).toUByteArray().toMutableList())
                    "LineNumberTable" -> LineNumberTable(attributeNameIndex, attributeLength, read2UBE(), input)
                    "LocalVariableTable" -> LocalVariableTable(attributeNameIndex, attributeLength, read2UBE(), input)
                    "LocalVariableTypeTable" -> LocalVariableTypeTable(attributeNameIndex, attributeLength, read2UBE(), input)
                    "Deprecated" -> Deprecated(attributeNameIndex, attributeLength)
                    "RuntimeVisibleAnnotations" -> RuntimeVisibleAnnotations(attributeNameIndex, attributeLength, read2UBE(), input)
                    "RuntimeInvisibleAnnotations" -> RuntimeInvisibleAnnotations(attributeNameIndex, attributeLength, read2UBE(), input)
                    "RuntimeVisibleParameterAnnotations" -> RuntimeVisibleParameterAnnotations(attributeNameIndex, attributeLength, read1(), input)
                    "RuntimeInvisibleParameterAnnotations" -> RuntimeInvisibleParameterAnnotations(attributeNameIndex, attributeLength, read1(), input)
                    "RuntimeVisibleTypeAnnotations" -> RuntimeVisibleTypeAnnotations(attributeNameIndex, attributeLength, read2UBE(), input)
                    "RuntimeInvisibleTypeAnnotations" -> RuntimeInvisibleTypeAnnotations(attributeNameIndex, attributeLength, read2UBE(), input)
                    "AnnotationDefault" -> AnnotationDefault(attributeNameIndex, attributeLength, ElementValuePair.ElementValue.read(input))
                    "BootstrapMethods" -> BootstrapMethods(attributeNameIndex, attributeLength, read2UBE(), input)
                    "MethodParameters" -> MethodParameters(attributeNameIndex, attributeLength, read1(), input)
                    "Module" -> Module(attributeNameIndex, attributeLength, read2UBE(), read2UBE(), read2UBE(), read2UBE(), input)
                    "ModulePackages" -> ModulePackages(attributeNameIndex, attributeLength, read2UBE(), input)
                    "ModuleMainClass" -> ModuleMainClass(attributeNameIndex, attributeLength, read2UBE())
                    "NestHost" -> NestHost(attributeNameIndex, attributeLength, read2UBE())
                    "NestMembers" -> NestMembers(attributeNameIndex, attributeLength, read2UBE(), input)
                    "Record" -> Record(attributeNameIndex, attributeLength, read2UBE(), input, constantPool)
                    "PermittedSubclasses" -> PermittedSubclasses(attributeNameIndex, attributeLength, read2UBE(), input)
                    else -> Attribute(attributeNameIndex, attributeLength, readNBytes(attributeLength.toInt()).toUByteArray())
                }
            }
        }

        data class ConstantValue(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var constantValueIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength, constantValueIndex.toUByteArrayBE())

        data class Code(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var maxStack: UShort = 0U,
            var maxLocals: UShort = 0U,
            var codeLength: UInt = 0U,
            var code: UByteArray = ubyteArrayOf(),
            var exceptionTableLength: UShort = 0U,
            var exceptionTable: MutableList<ExceptionTable> = mutableListOf(),
            var attributesCount: UShort = 0U,
            var attributes: MutableList<Attribute> = mutableListOf(),
            var constantPool: MutableList<Constant> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                input: InputStream,
                constantPool: MutableList<Constant> = mutableListOf()
            ) : this(
                attributeNameIndex,
                attributeLength,
                input.read2UBE(),
                input.read2UBE(),
                input.read4UBE(),
                input,
                constantPool
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                maxStack: UShort = 0U,
                maxLocals: UShort = 0U,
                codeLength: UInt = 0U,
                input: InputStream,
                constantPool: MutableList<Constant> = mutableListOf()
            ) : this(
                attributeNameIndex,
                attributeLength,
                maxStack,
                maxLocals,
                codeLength,
                input.readNBytes(codeLength.toInt()).toUByteArray(),
                input.read2UBE(),
                input,
                constantPool
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                maxStack: UShort = 0U,
                maxLocals: UShort = 0U,
                codeLength: UInt = 0U,
                code: UByteArray = ubyteArrayOf(),
                exceptionTableLength: UShort = 0U,
                input: InputStream,
                constantPool: MutableList<Constant> = mutableListOf()
            ) : this(
                attributeNameIndex,
                attributeLength,
                maxStack,
                maxLocals,
                codeLength,
                code,
                exceptionTableLength,
                (0 until exceptionTableLength.toInt()).map {
                    ExceptionTable(
                        input.read2UBE(),
                        input.read2UBE(),
                        input.read2UBE(),
                        input.read2UBE()
                    )
                }.toMutableList(),
                input.read2UBE(),
                input,
                constantPool
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                maxStack: UShort = 0U,
                maxLocals: UShort = 0U,
                codeLength: UInt = 0U,
                code: UByteArray = ubyteArrayOf(),
                exceptionTableLength: UShort = 0U,
                exceptionTable: MutableList<ExceptionTable> = mutableListOf(),
                attributesCount: UShort = 0U,
                input: InputStream,
                constantPool: MutableList<Constant> = mutableListOf()
            ) : this(
                attributeNameIndex,
                attributeLength,
                maxStack,
                maxLocals,
                codeLength,
                code,
                exceptionTableLength,
                exceptionTable,
                attributesCount,
                (0 until attributesCount.toInt()).map {
                    read(input, constantPool)
                }.toMutableList(),
                constantPool
            )

            data class ExceptionTable(
                var startPc: UShort = 0U,
                var endPc: UShort = 0U,
                var handlerPc: UShort = 0U,
                var catchType: UShort = 0U
            )
        }

        open class VerificationTypeInfo(
            open var tag: Tag = Tag.Top
        )
        {
            companion object
            {
                fun read(input: InputStream) = with (input) {
                    when (val tag = Tag.fromValue(read1()))
                    {
                        Tag.Top -> Top(tag)
                        Tag.Integer -> Integer(tag)
                        Tag.Float -> Float(tag)
                        Tag.Double -> Double(tag)
                        Tag.Long -> Long(tag)
                        Tag.Null -> Null(tag)
                        Tag.UninitializedThis -> UninitializedThis(tag)
                        Tag.Object -> Object(tag, read2UBE())
                        Tag.Uninitialized -> Uninitialized(tag, read2UBE())
                    }
                }
            }

            enum class Tag(val tag: UByte)
            {
                Top(0U),
                Integer(1U),
                Float(2U),
                Double(3U),
                Long(4U),
                Null(5U),
                UninitializedThis(6U),
                Object(7U),
                Uninitialized(8U);

                companion object
                {
                    fun fromValue(value: UByte) = entries.firstOrNull { it.tag == value }?: Top
                }
            }

            data class Top(
                override var tag: Tag = Tag.Top,
            ) : VerificationTypeInfo(tag)

            data class Integer(
                override var tag: Tag = Tag.Integer,
            ) : VerificationTypeInfo(tag)

            data class Float(
                override var tag: Tag = Tag.Float
            ) : VerificationTypeInfo(tag)

            data class Double(
                override var tag: Tag = Tag.Double
            ) : VerificationTypeInfo(tag)

            data class Long(
                override var tag: Tag = Tag.Long
            ) : VerificationTypeInfo(tag)

            data class Null(
                override var tag: Tag = Tag.Null
            ) : VerificationTypeInfo(tag)

            data class UninitializedThis(
                override var tag: Tag = Tag.UninitializedThis
            ) : VerificationTypeInfo(tag)

            data class Object(
                override var tag: Tag = Tag.Object,
                var cpoolIndex: UShort = 0U
            ) : VerificationTypeInfo(tag)

            data class Uninitialized(
                override var tag: Tag = Tag.Uninitialized,
                var offset: UShort = 0U
            ) : VerificationTypeInfo(tag)
        }

        data class StackMapTable(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numberOfEntries: UShort = 0U,
            var entries: MutableList<StackMapFrame> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                input.read2UBE(),
                input
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numberOfEntries: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numberOfEntries,
                (0 until numberOfEntries.toInt()).map {
                    StackMapFrame.read(input)
                }.toMutableList()
            )

            open class StackMapFrame(
                open var frameType: UByte = 0U,
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with(input) {
                        when (val frameType = read1())
                        {
                            in 0U..63U -> SameFrame(frameType)
                            in 64U..127U -> SameLocals1StackItemFrame(frameType, VerificationTypeInfo.read(this))
                            in 247U..247U -> SameLocals1StackItemFrameExtended(frameType, read2UBE(), VerificationTypeInfo.read(this))
                            in 248U..250U -> ChopFrame(frameType, read2UBE())
                            in 251U..251U -> SameFrameExtended(frameType, read2UBE())
                            in 252U..254U -> AppendFrame(frameType, read2UBE(), (0U until frameType - 251U).map { VerificationTypeInfo.read(this) }.toMutableList())
                            in 255U..255U -> FullFrame(frameType, read2UBE(), read2UBE(), input)
                            else -> StackMapFrame(frameType)
                        }
                    }
                }

                data class SameFrame(
                    override var frameType: UByte = 0U
                ) : StackMapFrame(frameType)

                data class SameLocals1StackItemFrame(
                    override var frameType: UByte = 64U,
                    var stack: VerificationTypeInfo = VerificationTypeInfo.Top()
                ) : StackMapFrame(frameType)

                data class SameLocals1StackItemFrameExtended(
                    override var frameType: UByte = 247U,
                    var offsetDelta: UShort = 0U,
                    var stack: VerificationTypeInfo = VerificationTypeInfo.Top()
                ) : StackMapFrame(frameType)

                data class ChopFrame(
                    override var frameType: UByte = 248U,
                    var offsetDelta: UShort = 0U
                ) : StackMapFrame(frameType)

                data class SameFrameExtended(
                    override var frameType: UByte = 251U,
                    var offsetDelta: UShort = 0U
                ) : StackMapFrame(frameType)

                data class AppendFrame(
                    override var frameType: UByte = 252U,
                    var offsetDelta: UShort = 0U,
                    var locals: MutableList<VerificationTypeInfo> = mutableListOf()
                ) : StackMapFrame(frameType)

                data class FullFrame(
                    override var frameType: UByte = 255U,
                    var offsetDelta: UShort = 0U,
                    var numberOfLocals: UShort = 0U,
                    var locals: MutableList<VerificationTypeInfo> = mutableListOf(),
                    var numberOfStackItems: UShort = 0U,
                    var stack: MutableList<VerificationTypeInfo> = mutableListOf()
                ) : StackMapFrame(frameType)
                {
                    constructor(
                        frameType: UByte = 255U,
                        offsetDelta: UShort = 0U,
                        numberOfLocals: UShort = 0U,
                        input: InputStream
                    ) : this(
                        frameType,
                        offsetDelta,
                        numberOfLocals,
                        (0 until numberOfLocals.toInt()).map {
                            VerificationTypeInfo.read(input)
                        }.toMutableList(),
                        input.read2UBE(),
                        input
                    )

                    constructor(
                        frameType: UByte = 255U,
                        offsetDelta: UShort = 0U,
                        numberOfLocals: UShort = 0U,
                        locals: MutableList<VerificationTypeInfo> = mutableListOf(),
                        numberOfStackItems: UShort = 0U,
                        input: InputStream
                    ) : this(
                        frameType,
                        offsetDelta,
                        numberOfLocals,
                        locals,
                        numberOfStackItems,
                        (0 until numberOfStackItems.toInt()).map {
                            VerificationTypeInfo.read(input)
                        }.toMutableList()
                    )
                }
            }
        }

        data class Exceptions(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numberOfExceptions: UShort = 0U,
            var exceptionIndexTable: MutableList<UShort> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numberOfExceptions: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numberOfExceptions,
                (0 until numberOfExceptions.toInt()).map {
                    input.read2UBE()
                }.toMutableList()
            )
        }

        data class InnerClasses(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numberOfClasses: UShort = 0U,
            var classes: MutableList<Classes> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numberOfClasses: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numberOfClasses,
                (0 until numberOfClasses.toInt()).map {
                    Classes.read(input)
                }.toMutableList()
            )

            data class Classes(
                var innerClassInfoIndex: UShort = 0U,
                var outerClassInfoIndex: UShort = 0U,
                var innerNameIndex: UShort = 0U,
                var innerClassAccesFlags: UShort = 0U
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Classes(
                            read2UBE(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }
        }

        data class EnclosingMethod(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var classIndex: UShort = 0U,
            var methodIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength)

        data class Synthetic(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
        ) : Attribute(attributeNameIndex, attributeLength)

        data class Signature(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var signatureIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength)

        data class SourceFile(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var sourceFileIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength)

        data class SourceDebugExtension(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var debugExtension: MutableList<UByte> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)

        data class LineNumberTable(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var lineNumberTableLength: UShort = 0U,
            var lineNumberTable: MutableList<LineNumber> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                lineNumberTableLength: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                lineNumberTableLength,
                (0 until lineNumberTableLength.toInt()).map {
                    LineNumber.read(input)
                }.toMutableList()
            )

            data class LineNumber(
                var startPc: UShort = 0U,
                var lineNumber: UShort = 0U
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        LineNumber(
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }
        }

        data class LocalVariableTable(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var localVariableTableLength: UShort = 0U,
            var localVariableTable: MutableList<LocalVariable> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                localVariableTableLength: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                localVariableTableLength,
                (0 until localVariableTableLength.toInt()).map {
                    LocalVariable.read(input)
                }.toMutableList()
            )

            data class LocalVariable(
                var startPc: UShort = 0U,
                var lineNumber: UShort = 0U,
                var nameIndex: UShort = 0U,
                var descriptorIndex: UShort = 0U,
                var index: UShort = 0U,
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        LocalVariable(
                            read2UBE(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }
        }

        data class LocalVariableTypeTable(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var localVariableTypeTableLength: UShort = 0U,
            var localVariableTypeTable: MutableList<LocalVariableType> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                localVariableTypeTableLength: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                localVariableTypeTableLength,
                (0 until localVariableTypeTableLength.toInt()).map {
                    LocalVariableType.read(input)
                }.toMutableList()
            )

            data class LocalVariableType(
                var startPc: UShort = 0U,
                var lineNumber: UShort = 0U,
                var nameIndex: UShort = 0U,
                var signatureIndex: UShort = 0U,
                var index: UShort = 0U,
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        LocalVariableType(
                            read2UBE(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }
        }

        data class Deprecated(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
        ) : Attribute(attributeNameIndex, attributeLength)

        data class Annotation(
            var typeIndex: UShort = 0U,
            var numElementValuePairs: UShort = 0U,
            var elementValuePairs: MutableList<ElementValuePair> = mutableListOf()
        )
        {
            companion object
            {
                fun read(input: InputStream) = with (input) {
                    Annotation(
                        read2UBE(),
                        read2UBE(),
                        input
                    )
                }
            }

            constructor(
                typeIndex: UShort = 0U,
                numElementValuePairs: UShort = 0U,
                input: InputStream
            ) : this(
                typeIndex,
                numElementValuePairs,
                (0 until numElementValuePairs.toInt()).map {
                    ElementValuePair.read(input)
                }.toMutableList()
            )
        }

        data class ElementValuePair(
            var elementNameIndex: UShort = 0U,
            var value: ElementValue
        )
        {
            companion object
            {
                fun read(input: InputStream) = with (input) {
                    ElementValuePair(
                        read2UBE(),
                        ElementValue.read(input)
                    )
                }
            }

            data class ElementValue(
                var tag: UByte = 0U,
                var valueItem: ElementValueItem = ElementValueItem()
            )
            {
                companion object
                {
                    fun read(input: InputStream): ElementValue = with (input) {
                        val tag = read1()
                        ElementValue(
                            tag,
                            when (tag.toInt().toChar())
                            {
                                'B' -> ElementValueItem.ConstValue(read2UBE())
                                'C' -> ElementValueItem.ConstValue(read2UBE())
                                'D' -> ElementValueItem.ConstValue(read2UBE())
                                'F' -> ElementValueItem.ConstValue(read2UBE())
                                'I' -> ElementValueItem.ConstValue(read2UBE())
                                'J' -> ElementValueItem.ConstValue(read2UBE())
                                'S' -> ElementValueItem.ConstValue(read2UBE())
                                'Z' -> ElementValueItem.ConstValue(read2UBE())
                                's' -> ElementValueItem.ConstValue(read2UBE())
                                'e' -> ElementValueItem.EnumConstValue(read2UBE())
                                'c' -> ElementValueItem.ClassInfo(read2UBE())
                                '@' -> ElementValueItem.AnnotationValue(Annotation.read(input))
                                '[' -> ElementValueItem.ArrayValue.read(input)
                                else -> ElementValueItem()
                            }
                        )
                    }
                }

                open class ElementValueItem
                {
                    data class ConstValue(
                        var constValueIndex: UShort = 0U
                    ) : ElementValueItem()

                    data class EnumConstValue(
                        var typeNameIndex: UShort = 0U,
                        var constValueIndex: UShort = 0U
                    ) : ElementValueItem()

                    data class ClassInfo(
                        var classInfoIndex: UShort = 0U
                    ) : ElementValueItem()

                    data class AnnotationValue(
                        var annotationValue: Annotation = Annotation()
                    ) : ElementValueItem()

                    data class ArrayValue(
                        var numValues: UShort = 0U,
                        var values: MutableList<ElementValue> = mutableListOf()
                    ) : ElementValueItem()
                    {
                        companion object
                        {
                            fun read(input: InputStream) = with (input) {
                                val numValues = read2UBE()
                                ArrayValue(
                                    numValues,
                                    (0 until numValues.toInt()).map {
                                        ElementValue.read(input)
                                    }.toMutableList()
                                )
                            }
                        }
                    }
                }
            }
        }

        data class RuntimeVisibleAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numAnnotations: UShort = 0U,
            var annotations: MutableList<Annotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numAnnotations: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numAnnotations,
                (0 until numAnnotations.toInt()).map {
                    Annotation.read(input)
                }.toMutableList()
            )
        }

        data class RuntimeInvisibleAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numAnnotations: UShort = 0U,
            var annotations: MutableList<Annotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numAnnotations: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numAnnotations,
                (0 until numAnnotations.toInt()).map {
                    Annotation.read(input)
                }.toMutableList()
            )
        }

        data class RuntimeVisibleParameterAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numParameters: UByte = 0U,
            var parameterAnnotations: MutableList<ParameterAnnotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numParameters: UByte = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numParameters,
                (0 until numParameters.toInt()).map {
                    ParameterAnnotation.read(input)
                }.toMutableList()
            )

            data class ParameterAnnotation(
                var numAnnotations: UShort = 0U,
                var annotations: MutableList<Annotation> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        val numAnnotations = read2UBE()
                        ParameterAnnotation(
                            numAnnotations,
                            (0 until numAnnotations.toInt()).map {
                                Annotation.read(input)
                            }.toMutableList()
                        )
                    }
                }
            }
        }

        data class RuntimeInvisibleParameterAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numParameters: UByte = 0U,
            var parameterAnnotations: MutableList<ParameterAnnotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numParameters: UByte = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numParameters,
                (0 until numParameters.toInt()).map {
                    ParameterAnnotation.read(input)
                }.toMutableList()
            )

            data class ParameterAnnotation(
                var numAnnotations: UShort = 0U,
                var annotations: MutableList<Annotation> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        val numAnnotations = read2UBE()
                        ParameterAnnotation(
                            numAnnotations,
                            (0 until numAnnotations.toInt()).map {
                                Annotation.read(input)
                            }.toMutableList()
                        )
                    }
                }
            }
        }

        data class TypeAnnotation(
            var targetType: UByte = 0U,
            var targetInfo: TargetInfo = TargetInfo(),
            var targetPath: TypePath = TypePath(),
            var typeIndex: UShort = 0U,
            var numElementValuePairs: UShort = 0U,
            var elementValuePairs: MutableList<ElementValuePair> = mutableListOf()
        )
        {
            companion object
            {
                fun read(input: InputStream) = with (input) {
                    val targetType = read1()
                    TypeAnnotation(
                        targetType,
                        TargetInfo.read(targetType, input),
                        TypePath.read(input),
                        read2UBE(),
                        read2UBE()
                    ).apply ctor@ {
                        this@ctor.elementValuePairs = (0 until this@ctor.numElementValuePairs.toInt()).map {
                            ElementValuePair.read(input)
                        }.toMutableList()
                    }
                }
            }

            open class TargetInfo
            {
                companion object
                {
                    fun read(targetType: UByte, input: InputStream) = with (input) {
                        when (targetType.toUInt())
                        {
                            0x00U -> TypeParameterTarget(read1())
                            0x01U -> TypeParameterTarget(read1())
                            0x10U -> SupertypeTarget(read2UBE())
                            0x11U -> TypeParameterBoundTarget(read1(), read1())
                            0x12U -> TypeParameterBoundTarget(read1(), read1())
                            0x13U -> EmptyTarget()
                            0x14U -> EmptyTarget()
                            0x15U -> EmptyTarget()
                            0x16U -> FormalParameterTarget(read1())
                            0x17U -> ThrowsTarget(read2UBE())
                            0x40U -> LocalvarTarget.read(input)
                            0x41U -> LocalvarTarget.read(input)
                            0x42U -> CatchTarget(read2UBE())
                            0x43U -> OffsetTarget(read2UBE())
                            0x44U -> OffsetTarget(read2UBE())
                            0x45U -> OffsetTarget(read2UBE())
                            0x46U -> OffsetTarget(read2UBE())
                            0x47U -> TypeArgumentTarget(read2UBE(), read1())
                            0x48U -> TypeArgumentTarget(read2UBE(), read1())
                            0x49U -> TypeArgumentTarget(read2UBE(), read1())
                            0x4AU -> TypeArgumentTarget(read2UBE(), read1())
                            0x4BU -> TypeArgumentTarget(read2UBE(), read1())
                            else -> EmptyTarget()
                        }
                    }
                }

                data class TypeParameterTarget(
                    var typeParameterIndex: UByte = 0U
                ) : TargetInfo()

                data class SupertypeTarget(
                    var superTypeIndex: UShort = 0U
                ) : TargetInfo()

                data class TypeParameterBoundTarget(
                    var typeParameterIndex: UByte = 0U,
                    var boundIndex: UByte = 0U
                ) : TargetInfo()

                data class EmptyTarget(
                    var nothing: Unit = Unit
                ) : TargetInfo()

                data class FormalParameterTarget(
                    var formalParameterIndex: UByte = 0U
                ) : TargetInfo()

                data class ThrowsTarget(
                    var throwsTypeIndex: UShort = 0U
                ) : TargetInfo()

                data class LocalvarTarget(
                    var tableLength: UShort = 0U,
                    var table: MutableList<Table> = mutableListOf()
                ) : TargetInfo()
                {
                    companion object
                    {
                        fun read(input: InputStream) = with (input) {
                            val tableLength = read2UBE()
                            LocalvarTarget(
                                tableLength,
                                (0 until tableLength.toInt()).map {
                                    Table.read(input)
                                }.toMutableList()
                            )
                        }
                    }

                    data class Table(
                        var startPc: UShort = 0U,
                        var length: UShort = 0U,
                        var index: UShort = 0U
                    )
                    {
                        companion object
                        {
                            fun read(input: InputStream) = with (input) {
                                Table(
                                    read2UBE(),
                                    read2UBE(),
                                    read2UBE()
                                )
                            }
                        }
                    }
                }

                data class CatchTarget(
                    var exceptionTableIndex: UShort = 0U
                ) : TargetInfo()

                data class OffsetTarget(
                    var offset: UShort = 0U
                ) : TargetInfo()

                data class TypeArgumentTarget(
                    var offset: UShort = 0U,
                    var typeArgumentIndex: UByte = 0U
                ) : TargetInfo()

            }

            data class TypePath(
                var pathLength: UByte = 0U,
                var path: MutableList<Path> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        val pathLength = read1()
                        TypePath(
                            pathLength,
                            (0 until pathLength.toInt()).map {
                                Path(read1(), read1())
                            }.toMutableList()
                        )
                    }
                }

                data class Path(
                    var typePathKind: UByte = 0U,
                    var typeArgumentIndex: UByte = 0U
                )
            }
        }

        data class RuntimeVisibleTypeAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numAnnotations: UShort = 0U,
            var annotations: MutableList<TypeAnnotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numAnnotations: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numAnnotations,
                (0 until numAnnotations.toInt()).map {
                    TypeAnnotation.read(input)
                }.toMutableList()
            )
        }

        data class RuntimeInvisibleTypeAnnotations(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numAnnotations: UShort = 0U,
            var annotations: MutableList<TypeAnnotation> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numAnnotations: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numAnnotations,
                (0 until numAnnotations.toInt()).map {
                    TypeAnnotation.read(input)
                }.toMutableList()
            )
        }

        data class AnnotationDefault(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var defaultValue: ElementValuePair.ElementValue = ElementValuePair.ElementValue()
        ) : Attribute(attributeNameIndex, attributeLength)

        data class BootstrapMethods(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numBootstrapMethods: UShort = 0U,
            var bootstrapMethods: MutableList<BootstrapMethod> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numBootstrapMethods: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numBootstrapMethods,
                (0 until numBootstrapMethods.toInt()).map {
                    BootstrapMethod.read(input)
                }.toMutableList()
            )

            data class BootstrapMethod(
                var bootstrapMethodRef: UShort = 0U,
                var numBootstrapArguments: UShort = 0U,
                var bootstrapArguments: MutableList<UShort> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        val bootstrapMethodRef = read2UBE()
                        val numBootstrapArguments = read2UBE()
                        BootstrapMethod(
                            bootstrapMethodRef,
                            numBootstrapArguments,
                            (0 until numBootstrapArguments.toInt()).map {
                                read2UBE()
                            }.toMutableList()
                        )
                    }
                }
            }
        }

        data class MethodParameters(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var parametersCount: UByte = 0U,
            var parameters: MutableList<Parameter> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                parametersCount: UByte = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                parametersCount,
                (0 until parametersCount.toInt()).map {
                    Parameter.read(input)
                }.toMutableList()
            )

            data class Parameter(
                var nameIndex: UShort = 0U,
                var accessFlags: UShort = 0U
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Parameter(
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }
        }

        data class Module(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var moduleNameIndex: UShort = 0U,
            var moduleFlags: UShort = 0U,
            var moduleVersionIndex: UShort = 0U,
            var requiresCount: UShort = 0U,
            var requires: MutableList<Require> = mutableListOf(),
            var exportsCount: UShort = 0U,
            var exports: MutableList<Export> = mutableListOf(),
            var opensCount: UShort = 0U,
            var opens: MutableList<Open> = mutableListOf(),
            var usesCount: UShort = 0U,
            var usesIndex: MutableList<UShort> = mutableListOf(),
            var providesCount: UShort = 0U,
            var provides: MutableList<Provide> = mutableListOf(),
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                moduleNameIndex: UShort = 0U,
                moduleFlags: UShort = 0U,
                moduleVersionIndex: UShort = 0U,
                requiresCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                moduleNameIndex,
                moduleFlags,
                moduleVersionIndex,
                requiresCount,
                (0 until requiresCount.toInt()).map {
                    Require.read(input)
                }.toMutableList(),
                input.read2UBE(),
                input
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                moduleNameIndex: UShort = 0U,
                moduleFlags: UShort = 0U,
                moduleVersionIndex: UShort = 0U,
                requiresCount: UShort = 0U,
                requires: MutableList<Require> = mutableListOf(),
                exportsCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                moduleNameIndex,
                moduleFlags,
                moduleVersionIndex,
                requiresCount,
                requires,
                exportsCount,
                (0 until exportsCount.toInt()).map {
                    Export.read(input)
                }.toMutableList(),
                input.read2UBE(),
                input
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                moduleNameIndex: UShort = 0U,
                moduleFlags: UShort = 0U,
                moduleVersionIndex: UShort = 0U,
                requiresCount: UShort = 0U,
                requires: MutableList<Require> = mutableListOf(),
                exportsCount: UShort = 0U,
                exports: MutableList<Export> = mutableListOf(),
                opensCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                moduleNameIndex,
                moduleFlags,
                moduleVersionIndex,
                requiresCount,
                requires,
                exportsCount,
                exports,
                opensCount,
                (0 until opensCount.toInt()).map {
                    Open.read(input)
                }.toMutableList(),
                input.read2UBE(),
                input
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                moduleNameIndex: UShort = 0U,
                moduleFlags: UShort = 0U,
                moduleVersionIndex: UShort = 0U,
                requiresCount: UShort = 0U,
                requires: MutableList<Require> = mutableListOf(),
                exportsCount: UShort = 0U,
                exports: MutableList<Export> = mutableListOf(),
                opensCount: UShort = 0U,
                opens: MutableList<Open> = mutableListOf(),
                usesCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                moduleNameIndex,
                moduleFlags,
                moduleVersionIndex,
                requiresCount,
                requires,
                exportsCount,
                exports,
                opensCount,
                opens,
                usesCount,
                (0 until usesCount.toInt()).map {
                    input.read2UBE()
                }.toMutableList(),
                input.read2UBE(),
                input
            )

            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                moduleNameIndex: UShort = 0U,
                moduleFlags: UShort = 0U,
                moduleVersionIndex: UShort = 0U,
                requiresCount: UShort = 0U,
                requires: MutableList<Require> = mutableListOf(),
                exportsCount: UShort = 0U,
                exports: MutableList<Export> = mutableListOf(),
                opensCount: UShort = 0U,
                opens: MutableList<Open> = mutableListOf(),
                usesCount: UShort = 0U,
                usesIndex: MutableList<UShort> = mutableListOf(),
                providesCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                moduleNameIndex,
                moduleFlags,
                moduleVersionIndex,
                requiresCount,
                requires,
                exportsCount,
                exports,
                opensCount,
                opens,
                usesCount,
                usesIndex,
                providesCount,
                (0 until usesCount.toInt()).map {
                    Provide.read(input)
                }.toMutableList()
            )

            data class Require(
                var requiresIndex: UShort = 0U,
                var requiresFlags: UShort = 0U,
                var requiresVersionIndex: UShort = 0U
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Require(
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        )
                    }
                }
            }

            data class Export(
                var exportsIndex: UShort = 0U,
                var exportsFlags: UShort = 0U,
                var exportsToCount: UShort = 0U,
                var exportsToIndex: MutableList<UShort> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Export(
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        ).apply ctor@ {
                            this@ctor.exportsToIndex = (0 until this@ctor.exportsToCount.toInt()).map {
                                read2UBE()
                            }.toMutableList()
                        }
                    }
                }
            }

            data class Open(
                var opensIndex: UShort = 0U,
                var opensFlags: UShort = 0U,
                var opensToCount: UShort = 0U,
                var opensToIndex: MutableList<UShort> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Open(
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        ).apply ctor@ {
                            this@ctor.opensToIndex = (0 until this@ctor.opensToCount.toInt()).map {
                                read2UBE()
                            }.toMutableList()
                        }
                    }
                }
            }

            data class Provide(
                var providesIndex: UShort = 0U,
                var providesWithCount: UShort = 0U,
                var providesWithIndex: MutableList<UShort> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream) = with (input) {
                        Provide(
                            read2UBE(),
                            read2UBE()
                        ).apply ctor@ {
                            this@ctor.providesWithIndex = (0 until this@ctor.providesWithCount.toInt()).map {
                                read2UBE()
                            }.toMutableList()
                        }
                    }
                }
            }
        }

        data class ModulePackages(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var packageCount: UShort = 0U,
            var packageIndex: MutableList<UShort> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                packageCount: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                packageCount,
                (0 until packageCount.toInt()).map {
                    input.read2UBE()
                }.toMutableList()
            )
        }

        data class ModuleMainClass(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var mainClassIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength)

        data class NestHost(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var hostClassIndex: UShort = 0U
        ) : Attribute(attributeNameIndex, attributeLength)

        data class NestMembers(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numberOfClasses: UShort = 0U,
            var classes: MutableList<UShort> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numberOfClasses: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numberOfClasses,
                (0 until numberOfClasses.toInt()).map {
                    input.read2UBE()
                }.toMutableList()
            )
        }

        data class Record(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var componentsCount: UShort = 0U,
            var components: MutableList<RecordComponentInfo> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                componentsCount: UShort = 0U,
                input: InputStream,
                constantPool: MutableList<Constant>
            ) : this(
                attributeNameIndex,
                attributeLength,
                componentsCount,
                (0 until componentsCount.toInt()).map {
                    RecordComponentInfo.read(input, constantPool)
                }.toMutableList()
            )

            data class RecordComponentInfo(
                var nameIndex: UShort = 0U,
                var descriptorInddex: UShort = 0U,
                var attributesCount: UShort = 0U,
                var attributes: MutableList<Attribute> = mutableListOf()
            )
            {
                companion object
                {
                    fun read(input: InputStream, constantPool: MutableList<Constant>) = with (input) {
                        RecordComponentInfo(
                            read2UBE(),
                            read2UBE(),
                            read2UBE()
                        ).apply ctor@ {
                            this@ctor.attributes = (0 until attributesCount.toInt()).map {
                                Attribute.read(input, constantPool)
                            }.toMutableList()
                        }
                    }
                }
            }
        }

        data class PermittedSubclasses(
            override var attributeNameIndex: UShort = 0U,
            override var attributeLength: UInt = 0U,
            var numberOfClasses: UShort = 0U,
            var classes: MutableList<UShort> = mutableListOf()
        ) : Attribute(attributeNameIndex, attributeLength)
        {
            constructor(
                attributeNameIndex: UShort = 0U,
                attributeLength: UInt = 0U,
                numberOfClasses: UShort = 0U,
                input: InputStream
            ) : this(
                attributeNameIndex,
                attributeLength,
                numberOfClasses,
                (0 until numberOfClasses.toInt()).map {
                    input.read2UBE()
                }.toMutableList()
            )
        }
    }
}
