
package com.example.podcasts.service;

import com.example.podcasts.model.AudioFile;
import com.example.podcasts.model.Episode;
import com.example.podcasts.model.PublicationSlot;
import com.example.podcasts.model.PublicationState;
import com.example.podcasts.repository.AudioFileRepository;
import com.example.podcasts.repository.EpisodeRepository;
import com.example.podcasts.repository.PodcastRepository;
import com.example.podcasts.repository.PublicationSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EpisodeService {

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private AudioFileRepository audioFileRepository;

    @Autowired
    private PublicationSlotRepository slotRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public List<Episode> getAll() {
        return episodeRepository.findAll();
    }

    @PreAuthorize("@authz.ownsEpisode(authentication, #id)")
    public Episode getById(Long id) {
        return episodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Эпизод не найден"
                ));
    }

    @Transactional
    @PreAuthorize("#data.podcastId != null and @authz.ownsPodcast(authentication, #data.podcastId)")
    public Episode create(Episode data) {
        if (data.getPodcastId() == null ||
                data.getTitle() == null ||
                data.getTitle().isBlank() ||
                data.getDurationSeconds() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нужно указать podcastId, title и durationSeconds"
            );
        }

        if (!podcastRepository.existsById(data.getPodcastId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Подкаст не найден"
            );
        }

        if (data.getDurationSeconds() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Длительность должна быть больше 0"
            );
        }

        Episode episode = new Episode();
        episode.setPodcastId(data.getPodcastId());
        episode.setTitle(data.getTitle());
        episode.setDurationSeconds(data.getDurationSeconds());
        episode.setPublicationState(PublicationState.DRAFT);

        return episodeRepository.save(episode);
    }

    @Transactional
    @PreAuthorize("@authz.ownsEpisode(authentication, #id) and (#data.podcastId == null or @authz.ownsPodcast(authentication, #data.podcastId))")
    public Episode update(Long id, Episode data) {
        Episode episode = getById(id);

        // Сначала проверяем новые данные
        if (data.getTitle() != null && data.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Название не может быть пустым"
            );
        }

        if (data.getDurationSeconds() != null &&
                data.getDurationSeconds() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Длительность должна быть больше 0"
            );
        }

        if (data.getPodcastId() != null &&
                !data.getPodcastId().equals(episode.getPodcastId())) {

            if (!podcastRepository.existsById(data.getPodcastId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Подкаст не найден"
                );
            }

            if (!slotRepository.findByEpisodeId(id).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "У эпизода есть слоты, перенести в другой подкаст нельзя"
                );
            }
        }

        // Применяем изменения
        if (data.getTitle() != null) {
            episode.setTitle(data.getTitle());
        }

        if (data.getDurationSeconds() != null) {
            episode.setDurationSeconds(data.getDurationSeconds());
        }

        if (data.getPodcastId() != null) {
            episode.setPodcastId(data.getPodcastId());
        }

        return episodeRepository.save(episode);
    }

    @Transactional
    @PreAuthorize("@authz.ownsEpisode(authentication, #id)")
    public void delete(Long id) {
        getById(id);

        // Удаляем аудиофайлы эпизода
        for (AudioFile file : audioFileRepository.findByEpisodeId(id)) {
            audioFileRepository.deleteById(file.getId());
        }

        // Удаляем слоты публикации
        for (PublicationSlot slot : slotRepository.findByEpisodeId(id)) {
            slotRepository.deleteById(slot.getId());
        }

        // Удаляем сам эпизод
        episodeRepository.deleteById(id);
    }
}
