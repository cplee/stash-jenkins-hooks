package com.nektos.stash.jenkins;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;

import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.atlassian.stash.ssh.api.SshCloneUrlResolver;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class RepositoryHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

	private final NavBuilder navBuilder;
	private final SshCloneUrlResolver sshResolver;
    private static final String REFS_HEADS = "refs/heads/";


	public RepositoryHook(NavBuilder navBuilder, SshCloneUrlResolver sshResolver) {
		this.navBuilder = navBuilder;
		this.sshResolver = sshResolver;
	}

	
    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
    	boolean includeBranch = (!context.getSettings().getBoolean("no_branch", false));
    	boolean useSSH = (sshResolver!=null && !context.getSettings().getBoolean("use_http", false));
        String url = context.getSettings().getString("jenkins_url");
        if(url == null || url.length()==0) {
        	url = System.getProperty("jenkins.url");
        }
        if (url != null) {
            try {

            	String scmUrl;
            	if(useSSH) {
            		URI uri = new URI(sshResolver.getCloneUrl(context.getRepository()));
            	    scmUrl=new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()).toASCIIString();
            	} else {
            		scmUrl=navBuilder.repo(context.getRepository()).clone("git").buildAbsoluteWithoutUsername();
            	}
				url += "/git/notifyCommit?url="+urlEncode(scmUrl);
            	if(includeBranch) {
	                url += "&branches="+urlEncode(Joiner.on(",").join(getUpdateBranches(refChanges)));
            	}
				System.out.println("Notifying '"+url+"'");
                new URL(url).openConnection().getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
    	String url = settings.getString("jenkins_url");
    	if(url==null  || url.isEmpty()) {
    		url = System.getProperty("jenkins.url");
    	}
    	if(url==null || url.isEmpty()) {
            errors.addFieldError("jenkins_url", "Jenkins URL field is blank, please supply one or specifiy a default with -Djenkins.url=http://my.jenkins.host");
        }
    }
    
    private static String urlEncode(String string) {
        try {
                return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
        }
}
    
    private Iterable<String> getUpdateBranches(Collection<RefChange> refChanges) {
        return Iterables.transform(Iterables.filter(refChanges, new Predicate<RefChange>() {
                @Override
                public boolean apply(RefChange input) {
                        // We only care about non-deleted branches
                        return input.getType() != RefChangeType.DELETE && input.getRefId().startsWith(REFS_HEADS);
                }
        }), new Function<RefChange, String>() {
                @Override
                public String apply(RefChange input) {
                        // Not 100% sure whether this is _just_ branch or is full ref?
                        return input.getRefId().replace(REFS_HEADS, "");
                }
        });
}
    
}
