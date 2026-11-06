package com.example.podcasts.security;

import com.example.podcasts.model.*;
import com.example.podcasts.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("authz")
public class AuthorizationService {
    private final UserRepository users;
    private final PodcastRepository podcasts;
    private final EpisodeRepository episodes;
    private final AudioFileRepository audioFiles;
    private final PublicationSlotRepository slots;

    public AuthorizationService(UserRepository users, PodcastRepository podcasts,
                                EpisodeRepository episodes, AudioFileRepository audioFiles,
                                PublicationSlotRepository slots) {
        this.users = users; this.podcasts = podcasts; this.episodes = episodes;
        this.audioFiles = audioFiles; this.slots = slots;
    }

    public boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isSelf(Authentication auth, Long userId) {
        return isAdmin(auth) || currentUserId(auth).equals(userId);
    }

    public boolean canCreatePodcast(Authentication auth, Long authorId) {
        return authorId != null && isSelf(auth, authorId);
    }

    public boolean ownsPodcast(Authentication auth, Long id) {
        if (isAdmin(auth)) return true;
        return podcasts.findById(id).map(Podcast::getAuthorId)
                .map(owner -> owner.equals(currentUserId(auth))).orElse(false);
    }

    public boolean ownsEpisode(Authentication auth, Long id) {
        if (isAdmin(auth)) return true;
        return episodes.findById(id).map(Episode::getPodcastId)
                .map(podcastId -> ownsPodcast(auth, podcastId)).orElse(false);
    }

    public boolean ownsAudioFile(Authentication auth, Long id) {
        if (isAdmin(auth)) return true;
        return audioFiles.findById(id).map(AudioFile::getEpisodeId)
                .map(episodeId -> ownsEpisode(auth, episodeId)).orElse(false);
    }

    public boolean ownsSlot(Authentication auth, Long id) {
        if (isAdmin(auth)) return true;
        return slots.findById(id).map(PublicationSlot::getPodcastId)
                .map(podcastId -> ownsPodcast(auth, podcastId)).orElse(false);
    }

    public Long currentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return -1L;
        return users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(-1L);
    }
}
