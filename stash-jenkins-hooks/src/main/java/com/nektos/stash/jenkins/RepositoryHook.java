package com.nektos.stash.jenkins;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;

import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;

public class RepositoryHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

	private final NavBuilder navBuilder;

	public RepositoryHook(NavBuilder navBuilder) {
		this.navBuilder = navBuilder;
	}

    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        String url = context.getSettings().getString("jenkins_url");
        if (url != null) {
            try {
            	String scmUrl = navBuilder.repo(context.getRepository()).clone("ssh").buildAbsolute();
				url += "/git/notifyCommit?url="+URLEncoder.encode(scmUrl,"UTF-8");
                new URL(url).openConnection().getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString("jenkins_url", "").isEmpty()) {
            errors.addFieldError("jenkins_url", "Jenkins URL field is blank, please supply one");
        }
    }
}
