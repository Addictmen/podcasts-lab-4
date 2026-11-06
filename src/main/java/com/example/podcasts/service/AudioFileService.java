
package com.example.podcasts.service;

import com.example.podcasts.model.AudioFile;
import com.example.podcasts.model.Episode;
import com.example.podcasts.model.PublicationState;
import com.example.podcasts.repository.AudioFileRepository;
import com.example.podcasts.repository.EpisodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AudioFileService {

    @Autowired
    private AudioFileRepository audioFileRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public List<AudioFile> getAll() {
        return audioFileRepository.findAll();
    }

    @PreAuthorize("@authz.ownsAudioFile(authentication, #id)")
    public AudioFile getById(Long id) {
        return audioFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Аудиофайл не найден"
                ));
    }

    @Transactional
    @PreAuthorize("#data.episodeId != null and @authz.ownsEpisode(authentication, #data.episodeId)")
    public AudioFile create(AudioFile data) {
        if (data.getEpisodeId() == null ||
                data.getPath() == null ||
                data.getPath().isBlank() ||
                data.getFormat() == null ||
                data.getSizeBytes() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нужно указать episodeId, path, format и sizeBytes"
            );
        }

        if (!episodeRepository.existsById(data.getEpisodeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Эпизод не найден"
            );
        }

        if (data.getSizeBytes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Размер должен быть больше 0"
            );
        }

        boolean firstFile =
                audioFileRepository.findByEpisodeId(data.getEpisodeId()).isEmpty();

        AudioFile file = new AudioFile();
        file.setEpisodeId(data.getEpisodeId());
        file.setPath(data.getPath());
        file.setFormat(data.getFormat());
        file.setSizeBytes(data.getSizeBytes());
        file.setMain(false);

        file = audioFileRepository.save(file);

        // Первый файл автоматически становится основным
        if (Boolean.TRUE.equals(data.getMain()) || firstFile) {
            makeMain(file);
        }

        return audioFileRepository.save(file);
    }

    @Transactional
    @PreAuthorize("@authz.ownsAudioFile(authentication, #id)")
    public AudioFile update(Long id, AudioFile data) {
        AudioFile file = getById(id);

        if (data.getPath() != null && data.getPath().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Путь не может быть пустым"
            );
        }

        if (data.getSizeBytes() != null && data.getSizeBytes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Размер должен быть больше 0"
            );
        }

        if (Boolean.FALSE.equals(data.getMain()) &&
                Boolean.TRUE.equals(file.getMain())) {
            checkCanRemoveMain(file.getEpisodeId());
        }

        if (data.getPath() != null) {
            file.setPath(data.getPath());
        }

        if (data.getFormat() != null) {
            file.setFormat(data.getFormat());
        }

        if (data.getSizeBytes() != null) {
            file.setSizeBytes(data.getSizeBytes());
        }

        if (data.getMain() != null) {
            if (data.getMain()) {
                makeMain(file);
            } else {
                file.setMain(false);
            }
        }

        return audioFileRepository.save(file);
    }

    @Transactional
    @PreAuthorize("@authz.ownsAudioFile(authentication, #id)")
    public void delete(Long id) {
        AudioFile file = getById(id);

        if (Boolean.TRUE.equals(file.getMain())) {
            AudioFile replacement = null;

            for (AudioFile other :
                    audioFileRepository.findByEpisodeId(file.getEpisodeId())) {

                if (!other.getId().equals(id)) {
                    replacement = other;
                    break;
                }
            }

            if (replacement == null) {
                checkCanRemoveMain(file.getEpisodeId());
            } else {
                replacement.setMain(true);
                audioFileRepository.save(replacement);
            }
        }

        audioFileRepository.deleteById(id);
    }

    private void checkCanRemoveMain(Long episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Эпизод не найден"
                ));

        if (episode.getPublicationState() != PublicationState.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Эпизод запланирован или опубликован, без основного файла нельзя"
            );
        }
    }

    private void makeMain(AudioFile file) {
        for (AudioFile other :
                audioFileRepository.findByEpisodeId(file.getEpisodeId())) {

            other.setMain(false);
            audioFileRepository.save(other);
        }

        file.setMain(true);
        audioFileRepository.save(file);
    }
}
