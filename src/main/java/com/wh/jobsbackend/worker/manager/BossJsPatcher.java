package com.wh.jobsbackend.worker.manager;

import lombok.extern.slf4j.Slf4j;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Slf4j
public class BossJsPatcher {

    private static class JsPatch {
        String name;
        Pattern pattern;
        String mode;
        String replacement;

        JsPatch(String name, String regex, String mode, String replacement) {
            this.name = name;
            this.pattern = Pattern.compile(regex);
            this.mode = mode;
            this.replacement = replacement;
        }
    }

    private static final List<JsPatch> PATCHES = new ArrayList<>();

    static {
        // mode = "body"
        PATCHES.add(new JsPatch("Bm", "function\\s+Bm\\s*\\(\\s*\\)\\s*\\{var\\s+e,\\s*t,\\s*n\\s*=\\s*Rm\\s*\\(\\s*\\)\\s*,\\s*i\\s*=\\s*window\\[", "body", "{}"));
        PATCHES.add(new JsPatch("function-t-encryptPwd", "function\\s+t\\s*\\(\\s*\\)\\s*\\{\\s*if\\s*\\(\\s*Sign\\.encryptPwd\\s*\\(\\s*\\)", "body", "{}"));

        // mode = "sub"
        PATCHES.add(new JsPatch("XCID-transpiled", "key:\"XCID\",value:function\\(\\)\\{", "sub", "key:\"XCID\",value:function(){return;"));
        PATCHES.add(new JsPatch("XCIT-transpiled", "key:\"XCIT\",value:function\\(\\)\\{", "sub", "key:\"XCIT\",value:function(){return;"));
        PATCHES.add(new JsPatch("XCID-es6", "\\bXCID\\(\\)\\{", "sub", "XCID(){return;"));
        PATCHES.add(new JsPatch("XCIT-es6", "\\bXCIT\\(\\)\\{", "sub", "XCIT(){return;"));
        PATCHES.add(new JsPatch("Rm", "function Rm\\(\\)\\{", "sub", "function Rm(){return;"));
        
        PATCHES.add(new JsPatch("bomb-Array", "new Array\\(1e\\d+\\)", "sub", "new Array(1)"));
        PATCHES.add(new JsPatch("bomb-repeat", "\\.repeat\\(1e\\d+\\)", "sub", ".repeat(1)"));
        
        PATCHES.add(new JsPatch("clear-arrow", "\\(\\)=>\\w+\\.clear\\(\\)", "sub", "()=>{}"));
        PATCHES.add(new JsPatch("clear-fn", "function\\(\\)\\{return \\w+\\.clear\\(\\)\\}", "sub", "function(){}"));
        PATCHES.add(new JsPatch("clear-assign", "(\\.table,\\w+=)\\w+\\.clear\\b", "sub", "$1function(){}"));
        PATCHES.add(new JsPatch("clear-comma", "(\\.table),\\w+\\.clear\\)", "sub", "$1,function(){})"));
    }

    public static String patch(String url, String text) {
        String newText = text;
        int patchCount = 0;

        for (JsPatch patch : PATCHES) {
            if ("sub".equals(patch.mode)) {
                Matcher m = patch.pattern.matcher(newText);
                if (m.find()) {
                    newText = m.replaceAll(patch.replacement);
                    patchCount++;
                }
            }
        }

        List<Object[]> bodyHits = new ArrayList<>();
        for (JsPatch patch : PATCHES) {
            if ("body".equals(patch.mode)) {
                Matcher m = patch.pattern.matcher(newText);
                while (m.find()) {
                    bodyHits.add(new Object[]{m.start(), patch});
                }
            }
        }
        
        bodyHits.sort(Comparator.comparingInt(o -> - (int)o[0])); // reverse order

        for (Object[] hit : bodyHits) {
            int start = (int) hit[0];
            JsPatch patch = (JsPatch) hit[1];
            int bodyStart = newText.indexOf('{', start) + 1;
            if (bodyStart > 0) {
                int end = findBalancedEnd(newText, bodyStart);
                String decl = newText.substring(start, bodyStart) + patch.replacement.substring(1);
                newText = newText.substring(0, start) + decl + newText.substring(end);
                patchCount++;
            }
        }

        if (patchCount > 0) {
            log.info("Patched Boss JS file: {} ({} patches applied)", url, patchCount);
        }
        return newText;
    }

    private static int findBalancedEnd(String s, int bodyStart) {
        int depth = 1;
        int i = bodyStart;
        int n = s.length();
        while (i < n && depth > 0) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == '"' || c == '\'') {
                char q = c;
                i++;
                while (i < n && s.charAt(i) != q) {
                    i += s.charAt(i) == '\\' ? 2 : 1;
                }
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int e = s.indexOf("*/", i + 2);
                i = e >= 0 ? e + 2 : n;
                continue;
            }
            i++;
        }
        return i;
    }
}
