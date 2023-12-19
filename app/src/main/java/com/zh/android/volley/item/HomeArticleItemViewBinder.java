package com.zh.android.volley.item;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zh.android.volley.R;
import com.zh.android.volley.model.HomeArticleModel;

import org.apache.commons.lang.StringEscapeUtils;

import me.drakeet.multitype.ItemViewBinder;

/**
 * 首页文章列表条目
 */
public class HomeArticleItemViewBinder extends ItemViewBinder<HomeArticleModel.PageModel.ItemModel, HomeArticleItemViewBinder.ViewHolder> {
    private final OnItemClickListener itemClickListener;

    public HomeArticleItemViewBinder(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    @NonNull
    @Override
    protected ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.item_home_article, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, HomeArticleModel.PageModel.ItemModel itemModel) {
        if (!TextUtils.isEmpty(itemModel.getTitle())) {
            //json里面有一些特殊符号，特殊符号会被gson转义，
            //StringEscapeUtils可以对转义的字符串进行反转义
            //调用StringEscapeUtils的unescapeHtml方法，如果字符串中没有转义字符，
            //unescapeHtml方法会直接返回原字符串，否则会对字符串进行反转义。
            String title = StringEscapeUtils.unescapeHtml(itemModel.getTitle());
            //标题
            holder.vTitle.setText(title);
        } else {
            holder.vTitle.setText("");
        }
        String name;
        if (!TextUtils.isEmpty(itemModel.getAuthor())) {
            name = itemModel.getAuthor();
        } else {
            name = itemModel.getShareUser();
        }
        //姓名
        holder.vUserName.setText(name);
        //日期
        holder.vDate.setText(itemModel.getNiceShareDate());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(itemClickListener != null) {
                    itemClickListener.onItemClick(holder.getPosition());
                }
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView vTitle;
        private final TextView vUserName;
        private final TextView vDate;

        public ViewHolder(View itemView) {
            super(itemView);
            vTitle = itemView.findViewById(R.id.title);
            vUserName = itemView.findViewById(R.id.user_name);
            vDate = itemView.findViewById(R.id.date);
        }
    }
}