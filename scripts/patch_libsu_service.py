#!/usr/bin/env python3
import argparse
import os
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


PATCHER_SOURCE = r"""
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LibsuServicePatcher {
    private static final String TARGET = "com/topjohnwu/superuser/internal/RootServiceManager.class";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: LibsuServicePatcher <in.jar> <out.jar>");
        }

        try (JarInputStream in = new JarInputStream(new java.io.FileInputStream(args[0]));
             JarOutputStream out = new JarOutputStream(new java.io.FileOutputStream(args[1]))) {
            JarEntry entry;
            boolean patched = false;
            while ((entry = in.getNextJarEntry()) != null) {
                JarEntry newEntry = new JarEntry(entry.getName());
                out.putNextEntry(newEntry);
                byte[] data = readAll(in);
                if (TARGET.equals(entry.getName())) {
                    data = patchClass(data);
                    patched = true;
                }
                out.write(data);
                out.closeEntry();
            }
            if (!patched) {
                throw new IllegalStateException("target class not found: " + TARGET);
            }
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        return out.toByteArray();
    }

    private static byte[] patchClass(byte[] input) {
        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    private boolean sawPump;
                    private boolean inserted;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                        if (opcode == Opcodes.INVOKESTATIC
                                && owner.equals("com/topjohnwu/superuser/internal/Utils")
                                && methodName.equals("pump")
                                && methodDescriptor.equals("(Ljava/io/InputStream;Ljava/io/OutputStream;)J")) {
                            sawPump = true;
                        } else if (sawPump
                                && !inserted
                                && opcode == Opcodes.INVOKEVIRTUAL
                                && owner.equals("java/io/OutputStream")
                                && methodName.equals("close")
                                && methodDescriptor.equals("()V")) {
                            super.visitVarInsn(Opcodes.ALOAD, 7);
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "setReadOnly", "()Z", false);
                            super.visitInsn(Opcodes.POP);
                            inserted = true;
                        }
                    }

                    @Override
                    public void visitEnd() {
                        if (sawPump && !inserted) {
                            throw new IllegalStateException("failed to patch RootServiceManager main.jar copy path");
                        }
                        super.visitEnd();
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }
}
"""


def extract_zip(src: Path, dst: Path) -> None:
    with zipfile.ZipFile(src) as zf:
        zf.extractall(dst)


def create_zip(src_dir: Path, dst: Path) -> None:
    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in sorted(src_dir.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(src_dir).as_posix())


def main() -> None:
    parser = argparse.ArgumentParser(description="Patch libsu-service main.jar to be read-only before app_process loads it.")
    parser.add_argument("input_aar", type=Path)
    parser.add_argument("output_aar", type=Path)
    parser.add_argument("--asm", type=Path, required=True)
    args = parser.parse_args()

    args.output_aar.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp_name:
        tmp = Path(tmp_name)
        aar_dir = tmp / "aar"
        patcher_dir = tmp / "patcher"
        aar_dir.mkdir()
        patcher_dir.mkdir()

        extract_zip(args.input_aar, aar_dir)
        source = patcher_dir / "LibsuServicePatcher.java"
        source.write_text(PATCHER_SOURCE, encoding="utf-8")

        subprocess.run(
            ["javac", "-cp", str(args.asm), str(source)],
            cwd=patcher_dir,
            check=True,
        )

        classes_jar = aar_dir / "classes.jar"
        patched_jar = tmp / "classes-patched.jar"
        subprocess.run(
            ["java", "-cp", os.pathsep.join([str(args.asm), str(patcher_dir)]), "LibsuServicePatcher", str(classes_jar), str(patched_jar)],
            cwd=patcher_dir,
            check=True,
        )
        shutil.move(patched_jar, classes_jar)
        create_zip(aar_dir, args.output_aar)


if __name__ == "__main__":
    main()
