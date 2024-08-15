# 变量定义
SOURCES = src
CLASSES = bin
JARS = ulib/antlr-4.7-complete.jar

# 规则定义
all: compile

# 编译所有 Java 源文件
compile:
	@mkdir -p $(CLASSES)
	@find $(SOURCES) -name "*.java" | xargs -I{} javac -cp $(JARS) -d $(CLASSES) {}

# 运行 Java 程序
run:
	@java -cp $(CLASSES):$(JARS) Main

# 清理编译产物
clean:
	@rm -rf $(CLASSES)

# 默认目标
.PHONY: all compile run clean
