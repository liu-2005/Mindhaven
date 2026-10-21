package com.mindhaven.service;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import com.mindhaven.common.BizException;
import com.mindhaven.config.AppProperties;
import com.mindhaven.entity.FileObject;
import com.mindhaven.repository.FileObjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceTests {
    @TempDir Path storage;
    private FileService service() {
        AppProperties properties = new AppProperties();
        properties.setStorageDir(storage.toString());
        FileObjectRepository files = mock(FileObjectRepository.class);
        when(files.save(any())).thenAnswer(call -> call.getArgument(0));
        return new FileService(files, properties);
    }
    @Test
    void rejectsHtmlEvenIfClientClaimsItIsPng() {
        assertThatThrownBy(() -> service().upload(1L,
                new MockMultipartFile("file", "cover.png", "image/png", "<script>alert(1)</script>".getBytes()), "ARTICLE", null, "cover"))
                .isInstanceOf(BizException.class);
    }
    @Test
    void canonicalizesImagesAndIgnoresSpoofedMime() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        FileService service = service();
        var saved = service.upload(1L, new MockMultipartFile("file", "cover.html", "text/html", output.toByteArray()), "ARTICLE", null, "cover");
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getStorageKey()).endsWith(".png");
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(service.readBytes(saved)))).isNotNull();
    }
    @Test
    void rejectsStoragePathsOutsideConfiguredRoot() {
        FileObject file = new FileObject();
        file.setStorageKey("../outside.txt");
        assertThatThrownBy(() -> service().readBytes(file)).isInstanceOf(BizException.class).hasMessageContaining("非法文件路径");
    }
}
