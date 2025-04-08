FROM alpine:3.12
LABEL author="Trevor Draeseke <tdraeseke@esri.com>"
ENV PYTHONUNBUFFERED=1
# Add scripts for the check.
ADD entry.py /entry.py
ADD style.rb /style.rb
ADD metadata_style_checker.py /metadata_style_checker.py
ADD README_style_checker.py /README_style_checker.py
# Install dependencies.
RUN echo "**** Install Ruby and mdl ****" && \
    apk add --update --no-cache ruby-full && \
    gem install mdl -v 0.12.0 --no-document && \  # Pinned mdl version to one compatible with Ruby 2.7
    echo "**** Install Python ****" && \
    apk add --no-cache python3 && \
    if [ ! -e /usr/bin/python ]; then ln -sf python3 /usr/bin/python ; fi
ENTRYPOINT ["python3", "/entry.py"]